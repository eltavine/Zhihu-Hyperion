#!/usr/bin/env python3
# Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
# Copyright (C) 2026, eltavine <me@eltavine.com>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation (version 3 only).
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

"""Writes the update.json that the app's UpdateController reads (feature/update).

Clients that are already installed keep reading this file, so fields may be added but never renamed, removed
or given a new meaning; an incompatible format needs a new file name. test_write_update_manifest.py and
UpdateManifestContractTest check both sides against the same sample.
"""

import argparse
import hashlib
import json
import pathlib

# update.json asset key -> release file name. The keys are the app's UpdateTarget keys.
ASSETS = {
    "android-lite": "zhihu-hyperion-lite.apk",
    "android-full": "zhihu-hyperion-full.apk",
    "windows-x64": "zhihu-hyperion-desktop-windows-x64.msi",
    "linux-x64": "zhihu-hyperion-desktop-linux-x64.AppImage",
    "macos-arm64": "zhihu-hyperion-desktop-macos-arm64.app.zip",
}


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dist", type=pathlib.Path, required=True,
                        help="Directory with the release files and the build-info.json written by package.yml.")
    parser.add_argument("--repository", required=True, help="owner/name of the GitHub repository.")
    parser.add_argument("--tag", required=True, help="Release tag the files are published under.")
    parser.add_argument("--commit", required=True, help="Commit the files were built from.")
    parser.add_argument("--notes", type=pathlib.Path, required=True, help="Markdown release notes.")
    args = parser.parse_args()

    build = json.loads((args.dist / "build-info.json").read_text(encoding="utf-8"))
    download_url = f"https://github.com/{args.repository}/releases/download/{args.tag}"
    manifest = {
        "versionName": build["versionName"],
        "versionCode": build["versionCode"],
        "commit": args.commit,
        "releaseUrl": f"https://github.com/{args.repository}/releases/tag/{args.tag}",
        "notes": args.notes.read_text(encoding="utf-8").strip(),
        "assets": {
            key: {
                "url": f"{download_url}/{name}",
                "size": (args.dist / name).stat().st_size,
                "sha256": sha256(args.dist / name),
            }
            for key, name in ASSETS.items()
        },
    }
    (args.dist / "update.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
