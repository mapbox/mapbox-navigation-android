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
        Index: libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
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
        diff --git a/changelog/unreleased/issues/example-known-issues.md b/changelog/unreleased/issues/example-known-issues.md
        new file mode 100644
        index 00000000000..c0505027151
        --- /dev/null
        +++ b/changelog/unreleased/issues/example-known-issues.md
        @@ -0,0 +1 @@
        +- It is an example of known issues
        \ No newline at end of file
        Index: libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
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
        diff = '''diff --git a/CHANGELOG.md b/CHANGELOG.md
        --- a/CHANGELOG.md	(revision 145c2d43cb3404f28975538ad440504d5ed74562)
        +++ b/CHANGELOG.md	(date 1659101646796)
        @@ -5,7 +5,6 @@
         ## Unreleased
         #### Features
         - Added first unreleased feature. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
        -- Added second unreleased feature. [#6048](https://github.com/mapbox/mapbox-navigation-android/pull/6048)

         #### Bug fixes and improvements
         - Fixed first unreleased bug. [#6047](https://github.com/mapbox/mapbox-navigation-android/pull/6047)
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), {"CHANGELOG.md": []})

    def test_extract_added_lines_only_changelog_has_added(self):
        expected = {
            "CHANGELOG.md": [
                '- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)']
        }
        diff = '''diff --git a/CHANGELOG.md b/CHANGELOG.md
        --- a/CHANGELOG.md	(revision fca6af9072a1b6cb7263460f7c3270e48bffed07)
        +++ b/CHANGELOG.md	(date 1659101777554)
        @@ -6,6 +6,7 @@
         #### Features
         - Added first unreleased feature. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
         - Added second unreleased feature. [#6048](https://github.com/mapbox/mapbox-navigation-android/pull/6048)
        +- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)

         #### Bug fixes and improvements
         - Fixed first unreleased bug. [#6047](https://github.com/mapbox/mapbox-navigation-android/pull/6047)
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), expected)

    def test_extract_added_lines_only_changelog_has_added_with_blank_lines(self):
        expected = {
            "CHANGELOG.md": [
                '- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)']
        }
        diff = '''diff --git a/CHANGELOG.md b/CHANGELOG.md
        --- a/CHANGELOG.md	(revision fca6af9072a1b6cb7263460f7c3270e48bffed07)
        +++ b/CHANGELOG.md	(date 1659101777554)
        @@ -6,6 +6,7 @@
         #### Features
         - Added first unreleased feature. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
         - Added second unreleased feature. [#6048](https://github.com/mapbox/mapbox-navigation-android/pull/6048)
        +- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)
        +
        +

         #### Bug fixes and improvements
         - Fixed first unreleased bug. [#6047](https://github.com/mapbox/mapbox-navigation-android/pull/6047)
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), expected)

    def test_extract_added_lines_only_changelog_with_path_has_added(self):
        expected = {
            "libnavui-androidauto/CHANGELOG.md": [
                '- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)']
        }
        diff = '''diff --git a/libnavui-androidauto/CHANGELOG.md b/libnavui-androidauto/CHANGELOG.md
        --- a/libnavui-androidauto/CHANGELOG.md	(revision ee5039502306c1ea6449f57615a2ad0f7f23fd83)
        +++ b/libnavui-androidauto/CHANGELOG.md	(date 1661336275515)
        @@ -6,6 +6,7 @@
         #### Features
         - Added first unreleased feature. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
         - Added second unreleased feature. [#6048](https://github.com/mapbox/mapbox-navigation-android/pull/6048)
        +- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)

         #### Bug fixes and improvements
         - Added. [#6165](https://github.com/mapbox/mapbox-navigation-android/pull/6165)
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), expected)

    def test_extract_added_lines_several_changelogs_in_a_row_has_added(self):
        expected = {
            "libnavui-androidauto/CHANGELOG.md": [
                '- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)'],
            "CHANGELOG.md": [
                '- Added fourth unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)']
        }
        diff = '''diff --git a/CHANGELOG.md b/CHANGELOG.md
        --- a/CHANGELOG.md	(revision e53b37ad3a4540bdb0878b4679a174f9518134b0)
        +++ b/CHANGELOG.md	(date 1661336645379)
        @@ -6,6 +6,7 @@
         #### Features
         - Added first unreleased feature. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
         - Added second unreleased feature. [#6048](https://github.com/mapbox/mapbox-navigation-android/pull/6048)
        +- Added fourth unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)

         #### Bug fixes and improvements
         - Added2. [#6145](https://github.com/mapbox/mapbox-navigation-android/pull/6145)
        Index: libnavui-androidauto/CHANGELOG.md
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/libnavui-androidauto/CHANGELOG.md b/libnavui-androidauto/CHANGELOG.md
        --- a/libnavui-androidauto/CHANGELOG.md	(revision 9307f85eaea82470de6f3111ff0218f76e1d9779)
        +++ b/libnavui-androidauto/CHANGELOG.md	(date 1661336328085)
        @@ -6,6 +6,7 @@
         #### Features
         - Added first unreleased feature. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
         - Added second unreleased feature. [#6048](https://github.com/mapbox/mapbox-navigation-android/pull/6048)
        +- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)

         #### Bug fixes and improvements
         - Added. [#6165](https://github.com/mapbox/mapbox-navigation-android/pull/6165)
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), expected)

    def test_extract_added_lines_several_changelogs_divided_has_added(self):
        expected = {
            "libnavui-androidauto/CHANGELOG.md": [
                '- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)'],
            "CHANGELOG.md": [
                '- Added fourth unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)']
        }
        diff = '''diff --git a/CHANGELOG.md b/CHANGELOG.md
        --- a/CHANGELOG.md	(revision e53b37ad3a4540bdb0878b4679a174f9518134b0)
        +++ b/CHANGELOG.md	(date 1661336645379)
        @@ -6,6 +6,7 @@
         #### Features
         - Added first unreleased feature. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
         - Added second unreleased feature. [#6048](https://github.com/mapbox/mapbox-navigation-android/pull/6048)
        +- Added fourth unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)

         #### Bug fixes and improvements
         - Added2. [#6145](https://github.com/mapbox/mapbox-navigation-android/pull/6145)
        Index: libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        --- a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(revision 85db37c032b85d89c65f909c65dbf4e36130bc95)
        +++ b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(date 1659101643213)
        @@ -4,5 +4,5 @@

             fun testMethod1() {}

        -    fun testMethod2(a: Int) = a * 10
        +    fun testMethod2(a: Int, b: Int) = a * 10 + b
         }
        Index: libnavui-androidauto/CHANGELOG.md
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/libnavui-androidauto/CHANGELOG.md b/libnavui-androidauto/CHANGELOG.md
        --- a/libnavui-androidauto/CHANGELOG.md	(revision 9307f85eaea82470de6f3111ff0218f76e1d9779)
        +++ b/libnavui-androidauto/CHANGELOG.md	(date 1661336328085)
        @@ -6,6 +6,7 @@
         #### Features
         - Added first unreleased feature. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
         - Added second unreleased feature. [#6048](https://github.com/mapbox/mapbox-navigation-android/pull/6048)
        +- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)

         #### Bug fixes and improvements
         - Added. [#6165](https://github.com/mapbox/mapbox-navigation-android/pull/6165)
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), expected)

    def test_extract_added_lines_several_changelogs_only_one_has_added(self):
        expected = {
            "CHANGELOG.md": [],
            "libnavui-androidauto/CHANGELOG.md": [
                '- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)']
        }
        diff = '''
        Index: CHANGELOG.md
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/CHANGELOG.md b/CHANGELOG.md
        --- a/CHANGELOG.md	(revision 2fed9e01ca0f2e15ce15f104a2175b12f48f15d4)
        +++ b/CHANGELOG.md	(date 1661341839927)
        @@ -5,7 +5,6 @@
         ## Unreleased
         #### Features
         - Added first unreleased feature. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
        -- Added second unreleased feature. [#6048](https://github.com/mapbox/mapbox-navigation-android/pull/6048)

         #### Bug fixes and improvements
         - Added2. [#6145](https://github.com/mapbox/mapbox-navigation-android/pull/6145)
        Index: libnavui-androidauto/CHANGELOG.md
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/libnavui-androidauto/CHANGELOG.md b/libnavui-androidauto/CHANGELOG.md
        --- a/libnavui-androidauto/CHANGELOG.md	(revision 4710fa5f28a132f6db42d1d580650a4b14987368)
        +++ b/libnavui-androidauto/CHANGELOG.md	(date 1661336328085)
        @@ -6,6 +6,7 @@
         #### Features
         - Added first unreleased feature. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
         - Added second unreleased feature. [#6048](https://github.com/mapbox/mapbox-navigation-android/pull/6048)
        +- Added third unreleased feature. [#6050](https://github.com/mapbox/mapbox-navigation-android/pull/6050)

         #### Bug fixes and improvements
         - Added. [#6165](https://github.com/mapbox/mapbox-navigation-android/pull/6165)
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), expected)

    def test_extract_added_lines_changelog_in_the_beginning_nothing_added(self):
        diff = '''
        Index: CHANGELOG.md
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/CHANGELOG.md b/CHANGELOG.md
        --- a/CHANGELOG.md	(revision 85db37c032b85d89c65f909c65dbf4e36130bc95)
        +++ b/CHANGELOG.md	(date 1659101873752)
        @@ -5,7 +5,6 @@
         ## Unreleased
         #### Features
         - Added first unreleased feature. [#6049](https://github.com/mapbox/mapbox-navigation-android/pull/6049)
        -- Added second unreleased feature. [#6048](https://github.com/mapbox/mapbox-navigation-android/pull/6048)

         #### Bug fixes and improvements
         - Fixed first unreleased bug. [#6047](https://github.com/mapbox/mapbox-navigation-android/pull/6047)
        Index: libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        --- a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(revision 85db37c032b85d89c65f909c65dbf4e36130bc95)
        +++ b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(date 1659101643213)
        @@ -4,5 +4,5 @@

             fun testMethod1() {}

        -    fun testMethod2(a: Int) = a * 10
        +    fun testMethod2(a: Int, b: Int) = a * 10 + b
         }
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), {"CHANGELOG.md": []})

    def test_extract_added_lines_changelog_in_the_beginning_has_added(self):
        expected = {
            "changelog/unreleased/issues/example-known-issues.md": [
                '- Updated the `MapboxRestAreaApi` logic to load a SAPA map only if the upcoming rest stop is at the current step of the route leg.'
            ]
        }
        diff = '''
        Index: CHANGELOG.md
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/changelog/unreleased/issues/example-known-issues.md b/changelog/unreleased/issues/example-known-issues.md
        new file mode 100644
        index 00000000000..c0505027151
        --- /dev/null
        +++ b/changelog/unreleased/issues/example-known-issues.md
        @@ -0,0 +1 @@
        +- Updated the `MapboxRestAreaApi` logic to load a SAPA map only if the upcoming rest stop is at the current step of the route leg.
        \ No newline at end of file
        Index: libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        --- a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(revision b96eff8c2c354263379ca588a17bbf4df6c802b8)
        +++ b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(date 1659104022448)
        @@ -4,5 +4,5 @@

             fun testMethod1() {}

        -    fun testMethod2(a: Int) = a * 10
        +    fun testMethod2(a: Int, b: Int) = a * 10 + b
         }
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), expected)

    def test_extract_added_lines_changelog_in_the_end_nothing_added(self):
        diff = '''
        Index: CHANGELOG.md
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/.circleci/config2.yml b/.circleci/config2.yml
        --- a/.circleci/config2.yml	(revision 2f1ddf175da5069b30d519119c52441f74a46974)
        +++ b/.circleci/config2.yml	(date 1659105154622)
        @@ -1,0 +1,1 @@
        +Added line
        Index: .circleci/config2.yml
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/CHANGELOG.md b/CHANGELOG.md
        --- a/CHANGELOG.md	(revision 2f1ddf175da5069b30d519119c52441f74a46974)
        +++ b/CHANGELOG.md	(date 1659105181666)
        @@ -9,7 +9,6 @@

         #### Bug fixes and improvements
         - Fixed first unreleased bug. [#6047](https://github.com/mapbox/mapbox-navigation-android/pull/6047)
        -- Fixed second unreleased bug. [#6046](https://github.com/mapbox/mapbox-navigation-android/pull/6046)

         ## Mapbox Navigation SDK 2.7.0-beta.1 - 14 July, 2022
         ### Changelog
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), {"CHANGELOG.md": []})

    def test_extract_added_lines_changelog_in_the_end_has_added(self):
        expected = {
            "CHANGELOG.md": [
                '- Fixed third unreleased bug. [#6045](https://github.com/mapbox/mapbox-navigation-android/pull/6045)']
        }
        diff = '''
        Index: .circleci/config2.yml
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/.circleci/config2.yml b/.circleci/config2.yml
        --- a/.circleci/config2.yml	(revision 03d0067b1a8fabbe70ccc9c8017c6c64a4d57cbb)
        +++ b/.circleci/config2.yml	(date 1659105387306)
        @@ -1,0 +1,1 @@
        +Added line
        Index: CHANGELOG.md
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/CHANGELOG.md b/CHANGELOG.md
        --- a/CHANGELOG.md	(revision ab3d6d6132f62cca268f5e664aecfc306d5c3fd5)
        +++ b/CHANGELOG.md	(date 1659105404056)
        @@ -9,6 +9,7 @@

         #### Bug fixes and improvements
         - Fixed first unreleased bug. [#6047](https://github.com/mapbox/mapbox-navigation-android/pull/6047)
        +- Fixed third unreleased bug. [#6045](https://github.com/mapbox/mapbox-navigation-android/pull/6045)
         - Fixed second unreleased bug. [#6046](https://github.com/mapbox/mapbox-navigation-android/pull/6046)

         ## Mapbox Navigation SDK 2.7.0-beta.1 - 14 July, 2022
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), expected)

    def test_extract_added_lines_changelog_in_the_middle_nothing_added(self):
        diff = '''
        Index: libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        --- a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(revision 0cf46e4ad84d2d914d1be07ac94401d1eb684ab5)
        +++ b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(date 1659105514618)
        @@ -4,5 +4,5 @@

             fun testMethod1() {}

        -    fun testMethod2(a: Int) = a * 10
        +    fun testMethod2(a: Int, b: Int) = a * 10 + b * 5
         }
        Index: CHANGELOG.md
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/CHANGELOG.md b/CHANGELOG.md
        --- a/CHANGELOG.md	(revision 2db5f9ac7a0be448ae6acc9108b722a75a8f1c7a)
        +++ b/CHANGELOG.md	(date 1659105622366)
        @@ -9,7 +9,6 @@

         #### Bug fixes and improvements
         - Fixed first unreleased bug. [#6047](https://github.com/mapbox/mapbox-navigation-android/pull/6047)
        -- Fixed second unreleased bug. [#6046](https://github.com/mapbox/mapbox-navigation-android/pull/6046)

         ## Mapbox Navigation SDK 2.7.0-beta.1 - 14 July, 2022
         ### Changelog
        Index: .circleci/config2.yml
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/.circleci/config2.yml b/.circleci/config2.yml
        --- a/.circleci/config2.yml	(revision 0cf46e4ad84d2d914d1be07ac94401d1eb684ab5)
        +++ b/.circleci/config2.yml	(date 1659105387306)
        @@ -1,0 +1,1 @@
        +Added line
        '''
        self.assertEqual(validate_changelog_utils.extract_added_lines(diff), {"CHANGELOG.md": []})

    def test_extract_added_lines_changelog_in_the_middle_has_added(self):
        expected = {
            "CHANGELOG.md": [
                '- Fixed third unreleased bug. [#6045](https://github.com/mapbox/mapbox-navigation-android/pull/6045)']
        }
        diff = '''
        Index: libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt
        --- a/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(revision 0cf46e4ad84d2d914d1be07ac94401d1eb684ab5)
        +++ b/libnavigation-core/src/main/java/com/mapbox/navigation/core/TestClass.kt	(date 1659105514618)
        @@ -4,5 +4,5 @@

             fun testMethod1() {}

        -    fun testMethod2(a: Int) = a * 10
        +    fun testMethod2(a: Int, b: Int) = a * 10 + b * 5
         }
        Index: CHANGELOG.md
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/CHANGELOG.md b/CHANGELOG.md
        --- a/CHANGELOG.md	(revision ab3d6d6132f62cca268f5e664aecfc306d5c3fd5)
        +++ b/CHANGELOG.md	(date 1659105418509)
        @@ -9,6 +9,7 @@

         #### Bug fixes and improvements
         - Fixed first unreleased bug. [#6047](https://github.com/mapbox/mapbox-navigation-android/pull/6047)
        +- Fixed third unreleased bug. [#6045](https://github.com/mapbox/mapbox-navigation-android/pull/6045)
         - Fixed second unreleased bug. [#6046](https://github.com/mapbox/mapbox-navigation-android/pull/6046)

         ## Mapbox Navigation SDK 2.7.0-beta.1 - 14 July, 2022
        Index: .circleci/config2.yml
        IDEA additional info:
        Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
        <+>UTF-8
        ===================================================================
        diff --git a/.circleci/config2.yml b/.circleci/config2.yml
        --- a/.circleci/config2.yml	(revision ab3d6d6132f62cca268f5e664aecfc306d5c3fd5)
        +++ b/.circleci/config2.yml	(date 1659105387306)
        @@ -1,0 +1,1 @@
        +Added line
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
