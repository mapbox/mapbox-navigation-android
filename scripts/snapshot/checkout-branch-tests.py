import unittest

from scripts.snapshot import utils


class TestCheckoutBranch(unittest.TestCase):

    def test_get_latest_tag(self):
        tags = [
            {'ref': 'refs/tags/v2.12.0-beta.3'},
            {'ref': 'refs/tags/v2.12.0-rc.1'},
            {'ref': 'refs/tags/androidauto-v0.22.0'},
            {'ref': 'refs/tags/v2.11.1'},
        ]
        self.assertEqual(utils.get_latest_tag(tags), 'v2.12.0-rc.1')

    def test_get_snapshot_branch_with_latest_alpha_tag(self):
        self.assertEqual(utils.get_snapshot_branch('v2.12.0-alpha.1'), 'main')

    def test_get_snapshot_branch_with_latest_beta_tag(self):
        self.assertEqual(utils.get_snapshot_branch('v2.12.0-beta.1'), 'main')

    def test_get_snapshot_branch_with_latest_rc_tag(self):
        self.assertEqual(utils.get_snapshot_branch('v2.12.0-rc.1'), 'release-v2.12')

    def test_get_snapshot_branch_with_latest_stable_tag(self):
        self.assertEqual(utils.get_snapshot_branch('v2.12.0'), 'main')


if __name__ == "__main__":
    unittest.main()
