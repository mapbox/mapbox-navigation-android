import os
import unittest

import validate_changelog_utils


class TestValidateChangelog(unittest.TestCase):

    def test_should_skip_changelog_no_labels(self):
        json = {}
        self.assertEqual(validate_changelog_utils.should_skip_changelog(json), False)

    def test_should_skip_changelog_no_skip_changelog_label(self):
        json = {"labels": [{"name": "some label"}]}
        self.assertEqual(validate_changelog_utils.should_skip_changelog(json), False)

    def test_should_skip_changelog_has_skip_changelog_label(self):
        json = {"labels": [{"name": "some label"}, {"name": "skip changelog"}]}
        self.assertEqual(validate_changelog_utils.should_skip_changelog(json), True)

    def test_check_has_changelog_diff_no_diff(self):
        diff = '''
        diff --git a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        --- a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(revision 25782d7748a343b2e4c85954cfa5c22846343f28)
        +++ b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(date 1659101434632)
        @@ -5,4 +5,6 @@
             fun testMethod1() {}

             fun testMethod2(a: Int) = a * 10
        +
        +    fun testMethod3(): String = ""
         }
        '''
        with self.assertRaises(Exception):
            validate_changelog_utils.check_has_changelog_diff(diff)

    def test_check_has_changelog_diff_has_diff(self):
        diff = '''
        diff --git a/changelog/unreleased/features/amazing-feature.md b/changelog/unreleased/features/amazing-feature.md
        new file mode 100644
        index 00000000000..c0505027151
        --- /dev/null
        +++ b/changelog/unreleased/features/amazing-feature.md
        @@ -0,0 +1 @@
        +- Definitely amazing feature
        +- Nice changes 
        \ No newline at end of file
        diff --git a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        --- a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(revision 25782d7748a343b2e4c85954cfa5c22846343f28)
        +++ b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(date 1659101434632)
        @@ -5,4 +5,6 @@
             fun testMethod1() {}

             fun testMethod2(a: Int) = a * 10
        +
        +    fun testMethod3(): String = ""
         }
        '''
        validate_changelog_utils.check_has_changelog_diff(diff)

    def test_check_has_changelog_diff_has_diff_bugfixes(self):
        diff = '''
        diff --git a/changelog/unreleased/bugfixes/amazing-fix.md b/changelog/unreleased/bugfixes/amazing-fix.md
        new file mode 100644
        index 00000000000..c0505027151
        --- /dev/null
        +++ b/changelog/unreleased/features/amazing-fix.md
        @@ -0,0 +1 @@
        +- Definitely amazing fix
        +- Nice changes 
        \ No newline at end of file
        '''
        validate_changelog_utils.check_has_changelog_diff(diff)

    def test_check_has_changelog_diff_has_diff_issues(self):
        diff = '''
        diff --git a/changelog/unreleased/issues/amazing-issue.md b/changelog/unreleased/issues/amazing-issue.md
        new file mode 100644
        index 00000000000..c0505027151
        --- /dev/null
        +++ b/changelog/unreleased/issues/amazing-issue.md
        @@ -0,0 +1 @@
        +- Definitely amazing issue
        +- Nice changes 
        \ No newline at end of file
        '''
        validate_changelog_utils.check_has_changelog_diff(diff)

    def test_check_has_changelog_diff_has_diff_other(self):
        diff = '''
        diff --git a/changelog/unreleased/other/amazing-other.md b/changelog/unreleased/other/amazing-other.md
        new file mode 100644
        index 00000000000..c0505027151
        --- /dev/null
        +++ b/changelog/unreleased/other/amazing-other.md
        @@ -0,0 +1 @@
        +- Definitely amazing other
        +- Nice changes 
        \ No newline at end of file
        '''
        validate_changelog_utils.check_has_changelog_diff(diff)

    def test_parse_contents_url_empty_json(self):
        with self.assertRaises(Exception):
            validate_changelog_utils.parse_contents_url([])

    def test_parse_contents_url_no_changelog(self):
        with self.assertRaises(Exception):
            validate_changelog_utils.parse_contents_url([{"filename": "not_changelog.md"}])

    def test_parse_contents_url_has_changelog_no_url(self):
        with self.assertRaises(Exception):
            validate_changelog_utils.parse_contents_url([{"filename": "CHANGELOG.md"}])

    def test_parse_contents_url_has_changelog_and_url(self):
        filename = "CHANGELOG.md"
        url = "my url"
        actual = validate_changelog_utils.parse_contents_url([{"filename": filename, "contents_url": url}])
        self.assertEqual(actual, {filename: url})

    def test_extract_added_lines_only_changelog_nothing_added(self):
        diff = '''
        diff --git a/changelog/unreleased/issues/example-known-issues.md b/changelog/unreleased/issues/example-known-issues.md
        new file mode 100644
        index 00000000000..c0505027151
        --- /dev/null
        +++ b/changelog/unreleased/issues/example-known-issues.md
        @@ -0,0 +1 @@
        - It is an example of known issues
        --
        \ No newline at end of file
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff),
                         {"changelog/unreleased/issues/example-known-issues.md": []})

    def test_extract_added_lines_only_changelog_has_added(self):
        expected = {
            "changelog/unreleased/issues/example-known-issues.md": ['- It is an example of known issues']
        }
        diff = '''
        diff --git a/changelog/unreleased/issues/example-known-issues.md b/changelog/unreleased/issues/example-known-issues.md
        new file mode 100644
        index 00000000000..c0505027151
        --- /dev/null
        +++ b/changelog/unreleased/issues/example-known-issues.md
        @@ -0,0 +1 @@
        +- It is an example of known issues
        \ No newline at end of file
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), expected)

    def test_extract_added_lines_only_changelog_has_added_with_blank_lines(self):
        expected = {
            "changelog/unreleased/issues/example-known-issues.md": ['- It is an example of known issues']
        }
        diff = '''
        diff --git a/changelog/unreleased/issues/example-known-issues.md b/changelog/unreleased/issues/example-known-issues.md
        new file mode 100644
        index 00000000000..c0505027151
        --- /dev/null
        +++ b/changelog/unreleased/issues/example-known-issues.md
        @@ -0,0 +1 @@
        +- It is an example of known issues
        +
        +
        +
        \ No newline at end of file
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), expected)

    def test_check_for_duplications_empty_list(self):
        validate_changelog_utils.check_for_duplications([])

    def test_check_for_duplications_single_unique_element(self):
        added_lines = ['- Line 1']
        validate_changelog_utils.check_for_duplications(added_lines)

    def test_check_for_duplications_single_repeated_element(self):
        added_lines = ['- Line 1', '- Line 1']
        with self.assertRaises(Exception):
            validate_changelog_utils.check_for_duplications(added_lines)

    def test_check_for_duplications_multiple_unique_elements(self):
        added_lines = ['- Line 1', '- Line 2', '- Line 3']
        validate_changelog_utils.check_for_duplications(added_lines)

    def test_check_for_duplications_multiple_elements_with_repeated(self):
        added_lines = ['- Line 1', '- Line 2', '- Line 3', '- Line 2']
        with self.assertRaises(Exception):
            validate_changelog_utils.check_for_duplications(added_lines)

    def read_test_changelog(self, filename):
        script_dir = os.path.dirname(__file__)
        rel_path = "test_resources/" + filename
        abs_file_path = os.path.join(script_dir, rel_path)
        with open(abs_file_path, 'r') as f:
            data = f.read()
        return data


if __name__ == "__main__":
    unittest.main()
