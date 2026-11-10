#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CHECK_DIR="$(mktemp -d)"
trap 'rm -rf "$CHECK_DIR"' EXIT

cd "$ROOT_DIR"
python3 tests/source_checks.py
python3 tests/localization_checks.py
python3 tests/style_checks.py
javac -d "$CHECK_DIR" tests/JavaSyntaxCheck.java
mapfile -d '' JAVA_FILES < <(find app/src -type f -name '*.java' -print0)
java -cp "$CHECK_DIR" JavaSyntaxCheck "${JAVA_FILES[@]}"
