#!/usr/bin/env node

const { createEntry } = require("./libs/changelog")
const minimist = require("minimist")


async function main() {
    try {
        const argv = minimist(process.argv.slice(2), {
            string: [
                'ticket',
                'type',
                'message'
            ],
            boolean: [
                'dry-run'
            ],
            alias: {
                'dry-run': 'isDryRun'
            },
            default: {
                isDryRun: false
            },
            unknown: function (name) {
                throw `parameter ${name} isn't supported`
            }
        });
        createEntry(argv)
    } catch(error) {
        console.error(error)
        console.log("Usage")
    }
}

main()