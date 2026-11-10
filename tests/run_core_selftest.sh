#!/usr/bin/env sh
set -eu

# Runs pure-Java checks without Android SDK. Requires a Bouncy Castle JAR.
if [ "$#" -ne 1 ]; then
    echo 'Usage: tests/run_core_selftest.sh /path/to/bcprov.jar' >&2
    exit 2
fi

root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
temporary_directory=$(mktemp -d)
trap 'rm -rf "$temporary_directory"' EXIT

javac -encoding UTF-8 -cp "$1" -d "$temporary_directory" \
    "$root"/app/src/main/java/org/gitzian/neurogranny/model/*.java \
    "$root"/app/src/main/java/org/gitzian/neurogranny/crypto/*.java \
    "$root"/app/src/main/java/org/gitzian/neurogranny/review/*.java \
    "$root"/app/src/main/java/org/gitzian/neurogranny/patterns/PatternDesign.java \
    "$root"/app/src/main/java/org/gitzian/neurogranny/backup/TitleMatching.java \
    "$root"/tests/CoreSelfTest.java \
    "$root"/tests/TitleMatchingSelfTest.java

java -cp "$temporary_directory:$1" CoreSelfTest
java -cp "$temporary_directory:$1" TitleMatchingSelfTest
