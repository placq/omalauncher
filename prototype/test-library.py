#!/usr/bin/env python3
"""Exercise the exact Python ROM scanner embedded in the face, without Android."""
import json
import pathlib
import subprocess
import sys
import tempfile
import unittest

face = pathlib.Path(__file__).with_name("pocket-gba.owl").read_text()
scanner = face.split('cmd("""python -c \'\n', 1)[1].split("\n' ${sh(folder)}", 1)[0]


def scan(folder):
    return json.loads(subprocess.check_output([sys.executable, "-c", scanner, str(folder)], text=True))


class LibraryTest(unittest.TestCase):
    def test_names_sorting_and_extension(self):
        with tempfile.TemporaryDirectory(dir="/tmp/opencode") as directory:
            folder = pathlib.Path(directory)
            for name in ["zelda.GBA", "A game.gba", "gra ' $(echo no).gba", "ignore.zip"]:
                (folder / name).touch()
            (folder / "directory.gba").mkdir()
            result = scan(folder)
            self.assertEqual("", result["error"])
            self.assertEqual(["A game", "gra ' $(echo no)", "zelda"], [g["name"] for g in result["items"]])
            self.assertTrue(all(pathlib.Path(g["path"]).is_absolute() for g in result["items"]))

    def test_missing_folder_is_visible_error(self):
        result = scan("/tmp/opencode/nonexistent-pocket-gba-library-test")
        self.assertEqual([], result["items"])
        self.assertTrue(result["error"])


if __name__ == "__main__":
    unittest.main()
