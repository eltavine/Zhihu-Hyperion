#!/usr/bin/env bash
# Builds a Release iOS app without an Apple account.
#
#   device    <out.ipa>  For iPhone and iPad, with code signing turned off. iOS installs it only after it is
#                        signed, for example by AltStore, SideStore or Sideloadly, or with your own certificate.
#   simulator <out.zip>  For simulators on Apple silicon Macs, signed ad hoc. Unzip it and run
#                        `xcrun simctl install booted Zhihu-Hyperion.app`.
set -euo pipefail

usage="usage: $0 device <out.ipa> | simulator <out.zip>"
kind="${1:-}"
output="${2:?$usage}"
case "$kind" in
  device)
    sdk=iphoneos
    destination='generic/platform=iOS'
    signing=(CODE_SIGNING_ALLOWED=NO)
    ;;
  simulator)
    sdk=iphonesimulator
    destination='generic/platform=iOS Simulator'
    signing=()
    ;;
  *)
    echo "$usage" >&2
    exit 2
    ;;
esac

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
mkdir -p "$(dirname "$output")"
output="$(cd "$(dirname "$output")" && pwd)/$(basename "$output")"
derived="$root/iosApp/build/derived-data"

# macOS ships bash 3.2, where expanding an empty array under `set -u` fails without the `+` guard.
xcodebuild -project "$root/iosApp/ZhihuHyperion.xcodeproj" -scheme ZhihuHyperion -configuration Release \
  -sdk "$sdk" -destination "$destination" -derivedDataPath "$derived" ${signing[@]+"${signing[@]}"} build

app="$derived/Build/Products/Release-$sdk/Zhihu-Hyperion.app"
test "$(/usr/bin/lipo -archs "$app/Zhihu-Hyperion")" = arm64

rm -f "$output"
if [[ "$kind" == device ]]; then
  staging="$(mktemp -d)"
  trap 'rm -rf "$staging"' EXIT
  mkdir "$staging/Payload"
  cp -R "$app" "$staging/Payload/"
  (cd "$staging" && /usr/bin/zip -qry "$output" Payload)
else
  /usr/bin/codesign --verify --deep --strict "$app"
  /usr/bin/ditto -c -k --sequesterRsrc --keepParent "$app" "$output"
fi
echo "Wrote $output"
