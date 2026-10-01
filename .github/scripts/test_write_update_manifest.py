#!/usr/bin/env python3
"""Checks write_update_manifest.py against the sample that the app's UpdateManifestContractTest decodes.

The sample is this test's expected output and that Kotlin test's input, so a format change made on either
side fails until both agree. Run with --update to rewrite the sample after an intended, compatible change.
"""

import json
import pathlib
import subprocess
import sys
import tempfile

SCRIPTS = pathlib.Path(__file__).resolve().parent
SAMPLE = SCRIPTS.parents[1] / "feature/update/src/jvmTest/resources/update.json"
sys.dont_write_bytecode = True
sys.path.insert(0, str(SCRIPTS))
from write_update_manifest import ASSETS  # noqa: E402


def generate() -> str:
    with tempfile.TemporaryDirectory() as temp:
        dist = pathlib.Path(temp)
        (dist / "build-info.json").write_text('{"versionName": "1.2.3", "versionCode": 1300}', encoding="utf-8")
        for name in ASSETS.values():
            (dist / name).write_bytes(name.encode())
        notes = dist / "notes.md"
        notes.write_text("- 修复更新检查 (abc1234)\n", encoding="utf-8")
        subprocess.run(
            [
                sys.executable, str(SCRIPTS / "write_update_manifest.py"),
                "--dist", str(dist),
                "--repository", "eltavine/Zhihu-Hyperion",
                "--tag", "nightly",
                "--commit", "0123456789abcdef0123456789abcdef01234567",
                "--notes", str(notes),
            ],
            check=True,
        )
        return (dist / "update.json").read_text(encoding="utf-8")


def main() -> None:
    actual = generate()
    if "--update" in sys.argv:
        SAMPLE.write_text(actual, encoding="utf-8")
        return
    if json.loads(actual) != json.loads(SAMPLE.read_text(encoding="utf-8")):
        print(f"write_update_manifest.py no longer produces {SAMPLE}:", file=sys.stderr)
        print(actual, file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
