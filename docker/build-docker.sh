#!/bin/sh
# Build an iSAQB curriculum inside Docker — no host JDK/Gradle needed.
#   build-docker.sh           # buildDocs (all languages and formats)
#   build-docker.sh pdfDE     # any Gradle task
set -eu

IMAGE="isaqb/curriculum-builder:local"

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/../.." && pwd)

[ "$#" -gt 0 ] || set -- buildDocs

docker build -t "$IMAGE" "$script_dir"

# -u maps output ownership to the caller; GRADLE_USER_HOME caches the toolchain across runs.
# --no-daemon: the daemon can't survive the --rm container, so a one-shot build is correct
# and avoids the "stopped/incompatible Daemons could not be reused" noise.
docker run --rm \
  -u "$(id -u):$(id -g)" \
  -v "$repo_root:/project" \
  -w /project \
  -e GRADLE_USER_HOME=/project/.gradle-docker \
  -e HOME=/project/.gradle-docker \
  "$IMAGE" "$@" --no-daemon
