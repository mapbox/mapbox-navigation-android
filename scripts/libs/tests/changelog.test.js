const assert = require('assert');
const fs = require('fs')
const path = require('path')

const testingDataDir = path.join(__dirname, 'testingData')

const { addReleaseNotesToChangelogMD } = require('../changelog')

describe('addReleaseNotesToChangelogMD', function() {
  it('adding new release notes to existing changelog file', function() {
    let existingFileContent = fs.readFileSync(path.join(testingDataDir, 'existing-changelog.md')).toString()
    let result = addReleaseNotesToChangelogMD(existingFileContent, "TEST_RELEASE_TEXT")
    let expectedChangelogContent = fs.readFileSync(path.join(testingDataDir, 'existing-changelog-with-new-release.md')).toString()
    assert.equal(result, expectedChangelogContent)
  })
});