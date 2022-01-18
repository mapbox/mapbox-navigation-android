#!/usr/bin/env node

const semver = require('semver')
const { compileChangeLog, compileReleaseNotesMd, removeEntries, addReleaseNotesToChangelogMD } = require("./libs/changelog")
const path = require('path');
const { execSync } = require('child_process');
const fs = require('fs');

let BUILD_DIR = path.join(".", ".build")
const CHANGELOG_PATH = path.join(".", "CHANGELOG.md")

try {
    main()
} catch (err) {
    console.error(err)
    console.error('Usage: scripts/changelog-pr --version [release version, for example 1.0.0] [--branch main] [--dry-run]');
}

function main() {
    let args = parseArguments()
    // TODO: check if GH exists
    // TODO: check if branch matches current one
    console.log("Generating dependencies.md")
    execSync("./gradlew createDependenciesMd")
    let dependenciesMd = fs.readFileSync(path.join(".", "build", "dependencies.md"))
    console.log("Generated dependencies.md")

    console.log("Compiling changelog")
    let changelog = compileReleaseNotesMd(args.version, dependenciesMd)
    
    let executor = args.isDryRun 
        ? execSyncDryRun
        : execSync

    let releaseNotesTempFile = path.join(BUILD_DIR, "RELEASENOTES.md")
    if (args.isDryRun) {
        console.log("Dry run. Generated changelog:")
        console.log(changelog)
    } else {
        fs.writeFileSync(releaseNotesTempFile, changelog)
        removeEntries()
        updateChangelogMDFile(changelog)
    } 
    
    executor(`git checkout -b add-changelog-${args.version}`)
    executor(`git add changelogs/unreleased CHANGELOG.md`) //TODO: leak of the path
    executor(`git commit -m "created changelog for ${args.version}"`)
    executor(`gh config set prompt disabled`)
    executor(`git push --set-upstream origin add-changelog-${args.version}`)
    executor(`gh pr create --base ${args.branch} --title "Release ${args.version}" --body "" --reviewer mapbox/navigation-android`)
    executor(`gh release create ${args.version} --draft --target ${args.branch} --notes-file ${releaseNotesTempFile} --title $VERSION`)
    executor(`git checkout ${args.branch}`)
}

function updateChangelogMDFile(newReleaseChangelog) {
    let existingChangelog = fs.readFileSync(CHANGELOG_PATH).toString()
    let updatedChangelog = addReleaseNotesToChangelogMD(existingChangelog, newReleaseChangelog)
    fs.writeFileSync(CHANGELOG_PATH, updatedChangelog)
}

function execSyncDryRun(command) {
    console.log(`dry-run: ${command}`)
}

function parseArguments() {
    const argv = require("minimist")(process.argv.slice(2), {
        string: [
            'branch',
            'version'
        ],
        boolean: [
            'dry-run'
        ],
        default : {
            branch: "main",
            'dry-run': false
        },
        unknown: function (name) {
            throw `parameter ${name} isn't supported`
        }
    });

    let result = { }

    result.isDryRun = argv['dry-run']
    result.branch = argv.branch

    result.version = argv.version
    if (result.version == undefined) {
        throw "you must specify a version for release"
    }
    if (!semver.valid(result.version)) {
        throw `passed version ${result.version} isn't SemVer compatible`
    }
    
    return result
}