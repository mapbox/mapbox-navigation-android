#!/usr/bin/env node

const semver = require('semver')
const { compileChangeLog, compileChangelog } = require("./changelog")
const path = require('path');


try {
    main()
} catch (err) {
    console.error(err)
    console.error('Usage: scripts/changelog-pr --version [release version, for example 1.0.0]  [--branch main]');
}

function main() {
    let args = parceArguments()
    const REPO_ROOT_DIR = path.join(__dirname, '..')
    const UNRELEASED_CHANGELOG_DIR = path.join(REPO_ROOT_DIR, 'changelogs', 'unreleased');
    let changelog = compileChangelog("")
}

function parceArguments() {
    const argv = require("minimist")(process.argv.slice(2), {
        string: [
            'branch',
            'version'
        ],
        unknown: function (name) {
            throw `parameter ${name} isn't supported`
        }
    });

    let result = { }

    if ('branch' in argv) {
        result.branch = argv.branch
    } else {
        result.branch = 'main'
    }

    result.version = argv.version
    if (result.version == undefined) {
        throw "you must specify a version for release"
    }
    if (!semver.valid(result.version)) {
        throw `passed version ${result.version} isn't SemVerCompatiable`
    }
    
    return result
}