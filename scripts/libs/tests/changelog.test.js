const mockfs = require('mock-fs');
const assert = require('assert');
const fs = require('fs')
const path = require('path')


const testingDataDir = path.join(__dirname, 'testingData')

const { addReleaseNotesToChangelogMD, compileReleaseNotesMd, createEntry } = require('../changelog')

describe('addReleaseNotesToChangelogMD', function () {
  it('adding new release notes to existing changelog file', function () {
    let existingFileContent = fs.readFileSync(path.join(testingDataDir, 'existing-changelog.md')).toString()
    let result = addReleaseNotesToChangelogMD(existingFileContent, "TEST_RELEASE_TEXT")
    let expectedChangelogContent = fs.readFileSync(path.join(testingDataDir, 'existing-changelog-with-new-release.md')).toString()
    assert.equal(result, expectedChangelogContent)
  })
});

describe('compile changelog', function () {
  it('compile changelog for issues of each type', async function () {
    let expectedResult = fs.readFileSync(path.join(testingDataDir, "expected-all-issues-types-changelog.md")).toString()
    await mockFileSystem(async function () {
      await createEntry(
        {
          ticket: 1,
          type: 'added',
          title: 'Test ticket 1'
        },
        "branch1"
      )
      await createEntry(
        {
          ticket: 2,
          type: 'fixed',
          title: 'Test ticket 2'
        },
        "branch2")
      await createEntry(
        {
          ticket: 3,
          type: 'added',
          title: 'Test ticket 3'
        },
        "branch3")
      await createEntry(
        {
          ticket: 4,
          type: 'fixed',
          title: 'Test ticket 4'
        },
        "branch4")
      let changelog = compileReleaseNotesMd({
        version: "2.1.0",
        dependenciesMd: "TEST_DEPENDENCIES",
        releaseDate: new Date(2022, 0, 9),
        fileCreationDataProvider: function (path) {
          return path.substring(path.length - 1)
        }
      })
      assert.equal(changelog, expectedResult)
    })
  })
})

async function mockFileSystem(block) {
  try {
    mockfs({})
    await block()
  } catch (error) {
    mockfs.restore()
    throw (error)
  }
}