#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

// https://keepachangelog.com/en/1.0.0/
const ENTRY_TYPES = ['added', 'changed', 'fixed', 'removed', 'deprecated', 'security'];
const REPO_ROOT_DIR = path.join(__dirname, '..')
const UNRELEASED_CHANGELOG_DIR = path.join(REPO_ROOT_DIR, 'changelogs', 'unreleased');

function compile() {
    const entries = {};
    for (const type of ENTRY_TYPES) {
        entries[type] = [];
    }

    for (const entryFile of fs.readdirSync(UNRELEASED_CHANGELOG_DIR)) {
        if (!entryFile.endsWith('.json')) { continue; }

        const entryFilePath = path.join(UNRELEASED_CHANGELOG_DIR, entryFile);
        const entry = JSON.parse(fs.readFileSync(entryFilePath));
        if (!isValidEntry(entry)) {
            console.warn(`Cannot use entry "${entryFile}"`);
            continue;
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

    console.log(output);
}

function isValidEntry(entry) {
    return entry.title && ENTRY_TYPES.includes(entry.type) && isInteger(entry.ticket);
}

function isInteger(str) {
    return !isNaN(str) && Number.isInteger(parseFloat(str));
}

String.prototype.capitalize = function () {
    return this.charAt(0).toUpperCase() + this.slice(1);
}

function processTitle(title) {
    // replace newlines with spaces to not break formatting in compiled changelog
    return title.replace(/\n/g, " ");
}

compile()