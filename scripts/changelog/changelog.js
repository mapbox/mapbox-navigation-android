#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const { getApp, getMetadata, getBranchName, getPullRequestNumber, isBranchProtectedOrRelease } = require('./github');
const parseGitPatch = require('parse-git-patch')
const prompts = require('prompts');
const semver = require('semver')

const argv = { }
// const argv = require("minimist")(process.argv.slice(2), {
//     string: [
//         'ticket',
//         'type',
//         'title'
//     ],
//     boolean: [
//         'compile',
//         'validate',
//         'push',
//         'help'
//     ],
//     alias: {
//         pr: 'ticket',
//         issue: 'ticket',
//         h: 'help'
//     },
//     unknown: function () {
//         usage()
//     }
// });

// https://keepachangelog.com/en/1.0.0/
const ENTRY_TYPES = ['added', 'changed', 'fixed', 'removed', 'deprecated', 'security'];

const REPO_ROOT_DIR = path.join('.')
const UNRELEASED_CHANGELOG_DIR = path.join(REPO_ROOT_DIR, 'changelogs', 'unreleased');

function findPRNumber() {
    try {
        const number = JSON.parse(execSync('gh pr view --json number', { stdio: 'pipe' }).toString()).number;
        return number || null;
    } catch (e) {
        return null;
    }
}

function lastCommitMessage() {
    // Using the commit header (i.e. the firsts line in the commit) for the template
    return execSync("git log -1 --pretty=%s")
        .toString()
        .trim()
}


function isInteger(str) {
    return !isNaN(str) && Number.isInteger(parseFloat(str));
}

function usage() {
    console.error('Usage: scripts/changelog [--ticket pr_number] [--type added|fixed|removed|changed] --title [entry title] [--push]');
    console.error('Or just simply run the tool without arguments and fill all fields interactively.');
    process.exit(1);
}

function isValidEntry(entry) {
    return entry.title && ENTRY_TYPES.includes(entry.type) && isInteger(entry.ticket);
}

String.prototype.capitalize = function () {
    return this.charAt(0).toUpperCase() + this.slice(1);
}

function processTitle(title) {
    // replace newlines with spaces to not break formatting in compiled changelog
    return title.replace(/\n/g, " ");
}

function compileChangelog() {
    const entries = {};
    for (const type of ENTRY_TYPES) {
        entries[type] = [];
    }

    for (const entryFile of fs.readdirSync(UNRELEASED_CHANGELOG_DIR)) {
        if (!entryFile.endsWith('.json')) { continue; }

        const entryFilePath = path.join(UNRELEASED_CHANGELOG_DIR, entryFile);
        const entry = JSON.parse(fs.readFileSync(entryFilePath));
        if (!isValidEntry(entry)) {
            throw `Cannot use entry "${entryFile}"`
        }
        entry['date'] = new Date(execSync(`git log -1 --format=%cd ${entryFilePath}`).toString().trim());
        entries[entry.type].push(entry);
    }

    for (const type of Object.keys(entries)) {
        entries[type].sort((a, b) => b.date - a.date);
    }

    const owner = 'mapbox';
    const repo = 'mapbox-navigation-android';

    let output = '';
    for (const type of ENTRY_TYPES) {
        const typeEntries = entries[type];
        if (typeEntries.length === 0) { continue; }
        output += `### ${type.capitalize()}\n`;

        for (const entry of typeEntries) {
            const title = processTitle(entry.title);
            output += `- ${title} [#${entry.ticket}](https://github.com/${owner}/${repo}/pull/${entry.ticket})\n`;
        }
    }

    return output;
}

function compileReleaseNotesMd(version, dependenciesMd) {
    var output = "##Changelog  "
    let major = semver.major(version)
    let minor = semver.minor(version)
    if (major == "2") {
        if (minor == "0") {
            output += "This is a patch release on top of v2.0.x which does not include changes introduced in v2.1.x and later.  "
        }
        output += "For details on how v2 differs from v1 and guidance on migrating from v1 of the Mapbox Navigation SDK for Android to the v2 public preview, see 2.0 Navigation SDK Migration Guide.  " 
    }
    output += compileChangelog()
    output += "### Mapbox dependencies  "
    output += "This release depends on, and has been tested with, the following Mapbox dependencies:  "
    output += dependenciesMd
    return output
}

function makeEntryPath(branchName) {
    return path.join(UNRELEASED_CHANGELOG_DIR, branchName.replace(/[^a-z0-9]/gi, '-') + '.json');
}

async function validate() {
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

    const entry = JSON.parse(fs.readFileSync(entryPath));
    if (!isValidEntry(entry)) {
        console.error(`Found changelog entry at ${entryPath}, but it has invalid format. Use ${__filename} script to generate it.`);
        process.exit(1);
    }

    // check that ticket exists
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

    console.log("All good. You have added changelog entry.");
}

async function askQuestions(entry) {
    const questions = [];
    if (!entry.ticket) {
        questions.push({
            type: 'number',
            name: 'ticket',
            initial: findPRNumber() || undefined,
            message: 'Ticket number(PR or issue): '
        });
    }
    if (!entry.type) {
        const choices = ENTRY_TYPES.map(x => { return { title: x.capitalize(), value: x }; });
        questions.push({
            type: 'select',
            name: 'type',
            message: 'Type:',
            choices: choices
        })
    }
    if (!entry.title) {
        questions.push({
            type: 'text',
            name: 'title',
            initial: lastCommitMessage(),
            message: 'Title:',
        })
    }
    return Object.assign(entry, await prompts(questions));
}

async function createEntry(push) {
    const title = argv.title;

    if (!isInteger(argv.ticket) && argv.ticket !== undefined) {
        console.error(`${argv.ticket} is not valid PR/issue number`);
        usage();
    }

    const ticket = Number(argv.ticket);
    const type = argv.type;

    if (type && !ENTRY_TYPES.includes(type)) {
        console.error(`${type} is not valid changelog entry type. Valid types: ${ENTRY_TYPES.join(', ')}`);
        usage();
    }

    const entry = await askQuestions({
        ticket: ticket,
        type: type,
        title: title
    });

    const branchName = getBranchName();

    if (branchName === 'master') {
        console.error('Cannot create changelog entry on master branch');
        process.exit(1);
    }

    const entryPath = makeEntryPath(branchName);

    fs.mkdirSync(path.dirname(entryPath), { recursive: true });
    fs.writeFileSync(entryPath, JSON.stringify(entry, null, 2));

    execSync(`git add ${entryPath}`);

    console.log(`Changelog entry created at ${entryPath}`);

    if (push) {
        try {
            execSync(`git commit ${entryPath} -m "Update changelog"`);
            execSync(`git push`);
        } catch (e) {
            console.error(`Cannot push: ${e}`);
        }

    }
}

async function main() {
    if (argv.help) {
        usage();
    }
    if (argv.compile && argv.validate) {
        console.error('Cannot --compile and --validate at the same time');
        process.exit(1);
    }
    if (argv.compile) {
        console.output(compileChangelog(UNRELEASED_CHANGELOG_DIR));
    } else if (argv.validate) {
        await validate();
    } else {
        await createEntry(argv.push);
    }
}

//main();

module.exports = {
    compileReleaseNotesMd
};