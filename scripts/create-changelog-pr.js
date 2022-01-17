#!/usr/bin/env node

const semver = require('semver')
const { compileChangeLog, compileReleaseNotesMd } = require("./libs/changelog")
const path = require('path');
const { execSync } = require('child_process');
const fs = require('fs');


try {
    main()
} catch (err) {
    console.error(err)
    console.error('Usage: scripts/changelog-pr --version [release version, for example 1.0.0] [--branch main] ');
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
    console.log(changelog)
}

function parseArguments() {
    const argv = require("minimist")(process.argv.slice(2), {
        string: [
            'branch',
            'version',
            'dependenciesMdFile'
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
        throw `passed version ${result.version} isn't SemVer compatible`
    }
    
    return result
}