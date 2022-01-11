#!/usr/bin/env node

/* eslint-disable */

const { execSync } = require('child_process');
const { Octokit } = require("@octokit/rest");

const execute = cmd => execSync(cmd).toString().trim();
const git = cmd => execute(`git ${cmd}`);

if (process.env.CI) {
    process.on('unhandledRejection', error => {
        // don't log `error` directly, because errors from child_process.execSync
        // contain an (undocumented) `envPairs` with environment variable values
        console.log(error.message || 'Error');
        process.exit(1)
    });
}

const owner = 'mapbox';
const repo = 'mapbox-navigation-android';

let metadata;
let extendedMetadata;

function getMetadata() {
    if (!metadata) {
        const sha = getHeadSha();
        const branch = getBranchName();
        console.warn(`      Head: ${sha} (${branch})`);
        metadata = { owner, repo, sha, branch };
    }
    return metadata;
}

function getExtendedMetadata() {
    if (!extendedMetadata) {
        extendedMetadata = Object.assign({}, getMetadata());
        extendedMetadata.build_number = getBuildNumber();
        extendedMetadata.author = getCommitAuthor(extendedMetadata.sha);
        extendedMetadata.timestamp = getCommitTimestamp(extendedMetadata.sha);
        extendedMetadata.datetime = getCommitDate(extendedMetadata.sha, extendedMetadata.timestamp);
        extendedMetadata.subject = getCommitSubject(extendedMetadata.sha);
        extendedMetadata.tags = getCommitTags(extendedMetadata.sha);
    }
    return extendedMetadata;
}

const apps = {};

function isExpiredApp(app) {
    // IT recomments to update token every 45 minutes
    const EXPIRATION_PERIOD_MS = 45 * 60 * 1000;
    // `app.creationTimestamp` is timestamp in the past, so substract it from current time to obtain positive value
    return ((new Date()).getTime() - app.creationTimestamp.getTime()) > EXPIRATION_PERIOD_MS;
}

// `id` can be one of: 'writer', 'reader', 'issues', 'notifier'
async function getApp(id) {
    if (!(id in apps) || isExpiredApp(apps[id])) {
        console.log(`Requesting GitHub token for ${id}`);

        const token = execute(`mbx-ci github ${id} token`);
        apps[id] = {
            app: new Octokit({auth: token}),
            creationTimestamp: new Date()
        };

        await apps[id].app.auth();
    }
    return apps[id].app;
}

function getHeadSha() {
    return process.env['CIRCLE_SHA1'] || git(`rev-parse --verify HEAD`);
}

function getBranchName() {
    return process.env['CIRCLE_BRANCH'] || git("rev-parse --verify --abbrev-ref HEAD");
}

function getBuildNumber() {
    return +(process.env['CIRCLE_BUILD_NUM'] || 0);
}

function getCommitAuthor(commitSha) {
    return git(`show -s --format="%an" ${commitSha}`);
}

function getCommitDate(commitSha, timestamp = null) {
    timestamp = timestamp || getCommitTimestamp(commitSha);
    return new Date(1000 * timestamp);
}

function getCommitTimestamp(commitSha) {
    return +git(`show -s --format="%ct" ${commitSha}`);
}

function getCommitSubject(commitSha) {
    return git(`show -s --format="%s" ${commitSha}`);
}

function getCommitTags(commitSha) {
    return git(`tag --points-at ${commitSha}`).split('\n').filter(tag => tag.length);
}

async function getPullRequestNumber() {
    const prURL = process.env['CIRCLE_PULL_REQUEST'];
    // we are lucky, we have PR url provided by Circle CI
    if (prURL) {
        const pullNumber = prURL.substring(prURL.lastIndexOf('/') + 1);
        return Number.parseInt(pullNumber);
    }

    // try to find PR by branch name using GitHub API
    const app = await getApp('reader');
    const {data: prs} = await app.pulls.list({
        owner,
        repo,
        state: 'open',
        head: `mapbox:${getBranchName()}`
    });

    if (prs.length === 1) {
        return prs[0].number;
    } else if (!prs.length) {
        console.log('No PR found.');
    } else {
        console.log(`Found ${prs.length} PRs. Not clear which one to choose.`);
    }

    return null;
}

async function getPullRequestData() {
    const pull_number = await getPullRequestNumber();
    if (!pull_number) {
        return null;
    }
    const app = await getApp('reader');
    const {data} = await app.pulls.get({owner, repo, pull_number});
    return data;
}

function getMergeBaseByBranch(baseBranch) {
    const {sha: headSha} = getMetadata();
    const mergeBase = git(`merge-base origin/${baseBranch} ${headSha}`);
    console.warn(`Merge base: ${mergeBase}`);
    return mergeBase;
}

async function getMergeBase() {
    const prData = await getPullRequestData();
    if (prData) {
        console.warn(`      Base: ${prData.base.sha} (${prData.base.ref})`);
        return getMergeBaseByBranch(prData.base.ref);
    }

    const {sha: headSha} = getMetadata();
    const n = 10;
    console.warn('            (using heuristic for finding merge base in master|support|release branch)');
    // Walk backward through the history (maximum of 10 commits) until
    // finding a commit on either master or release-*; assume that's the
    // base branch.
    for (const sha of git(`rev-list --max-count=${n} ${headSha}`).split('\n')) {
        const baseSha = git(`branch -r --contains ${sha} origin/master`).split('\n')[0].trim().replace(/^origin\//, '');
        if (baseSha) {
            const mergeBase = git(`merge-base origin/${baseSha} ${headSha}`);
            console.warn(`Merge base: ${mergeBase}`);
            return mergeBase;
        }
    }
    console.warn(`(unable to find merge base in the last ${n} commits of the master|support|release branch)`);
    return null;
}

// useful to not run particular checks against protected branches(e.g. master) or on releases
async function isBranchProtectedOrRelease() {
    if (process.env['CIRCLE_TAG']) { // running on the tag (i.e. release)
        return true;
    }
    const { owner, repo, branch } = getMetadata();

    const app = await getApp('reader');

    const { data: { protected: branchProtectionEnabled } } = await app.repos.getBranch({
        owner,
        repo,
        branch
    });
    return branchProtectionEnabled;
}

module.exports = {
    getApp,
    getMetadata,
    getExtendedMetadata,
    getBranchName,
    getHeadSha,
    getBuildNumber,
    getCommitAuthor,
    getCommitTimestamp,
    getCommitDate,
    getCommitSubject,
    getCommitTags,
    getPullRequestNumber,
    getPullRequestData,
    getMergeBaseByBranch,
    getMergeBase,
    isBranchProtectedOrRelease
};