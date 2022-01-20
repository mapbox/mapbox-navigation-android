#!/usr/bin/env node

const { getApp, getMetadata, getBranchName, getPullRequestNumber, isBranchProtectedOrRelease } = require('./libs/github');
const { isValidChangelogEntries, makeEntryPath } = require("./libs/changelog");
const parseGitPatch = require('parse-git-patch')
const fs = require('fs');


async function main() {
    const { owner, repo, branch } = getMetadata();
    if (await isBranchProtectedOrRelease()) {
        console.log(`Running on protected '${branch}' branch. Skipping changelog check.`)
        process.exit(0);
    }


    const pullNumber = await getPullRequestNumber();

    if (!pullNumber) {
        console.error("Couldn't check changelog, because PR is not created yet.");
        process.exit(1);
    }

    const app = await getApp('reader');

    const { data: pr } = await app.pulls.get({
        owner,
        repo,
        pull_number: pullNumber
    });

    for (const label of pr.labels) {
        if (label.name === "skip changelog") {
            console.log("Found 'skip changelog' label. Skip changelog check.");
            return;
        }
    }

    const { data: prPatch } = await app.pulls.get({
        owner,
        repo,
        pull_number: pullNumber,
        mediaType: {
            format: "patch",
        },
    });

    const parsedData = parseGitPatch(prPatch);

    const entryPath = makeEntryPath(getBranchName());

    if (parsedData.files.some(file => file.beforeName === 'CHANGELOG.md' && file.modifiedLines.length > 0)) {
        console.error(`
            You have changed CHANGELOG.md. 
            It is managed automatically at the moment. 
            Probably you supposed to add new changelog entry to ${UNRELEASED_CHANGELOG_DIR} directory. 
            You can do it using ${__filename} script.
            If you intentionally did it, please add "skip changelog" label to PR.
        `);
        process.exit(1);
    }

    if (!parsedData.files.some(file => entryPath.endsWith(file.afterName) && !file.deleted)) {
        console.error(`Expected changelog entry at ${entryPath} to be modified. Use ${__filename} script to generate it.`);
        process.exit(1);
    }

    if (!fs.existsSync(entryPath)) {
        console.error(`Cannot find changelog entry for PR. Expected to find it at ${entryPath}. Use ${__filename} script to generate it.`);
        process.exit(1);
    }

    const entries = JSON.parse(fs.readFileSync(entryPath));
    if (!isValidChangelogEntries(entries)) {
        console.error(`Found changelog entry at ${entryPath}, but it has invalid format. Use ${__filename} script to generate it.`);
        process.exit(1);
    }

    // check that ticket exists
    for(const entry of entries) {
        try {
            await app.issues.get({
                owner,
                repo,
                issue_number: entry.ticket
            });
        } catch (e) {
            console.error(`Ticket #${entry.ticket} does not exist on github from changelog entry at ${entryPath}.`);
            process.exit(1);
        }
    }

    console.log("All good. You have added changelog entry.");
}

main()