#!/usr/bin/env bash
#
# Run the Playwright WebKit project inside the official Playwright container.
#
# Playwright's WebKit bundle is built for Ubuntu 24.04 and links against older
# sonames (libicu*.so.74, libjpeg.so.8, libjxl.so.0.8, libbacktrace.so.0) that
# Fedora does not ship, so it cannot run natively on Fedora. The container
# provides the matching runtime without touching the host.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

engine="${CONTAINER_ENGINE:-}"
if [[ -z "$engine" ]]; then
  if command -v podman >/dev/null 2>&1; then
    engine=podman
  elif command -v docker >/dev/null 2>&1; then
    engine=docker
  else
    echo "error: podman or docker is required to run WebKit tests" >&2
    exit 1
  fi
fi

version="$(node -p "require('@playwright/test/package.json').version")"
image="${PLAYWRIGHT_IMAGE:-mcr.microsoft.com/playwright:v${version}-noble}"

# The run uses the same preview server as CI, so build first unless asked not to.
if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  bun run build
fi

engine_args=(--rm --init --ipc=host --security-opt label=disable)
if [[ "$engine" == "podman" ]]; then
  engine_args+=(--userns=keep-id)
else
  engine_args+=(--user "$(id -u):$(id -g)" -e HOME=/tmp)
fi

exec "$engine" run "${engine_args[@]}" \
  -v "$root:/work" \
  -w /work \
  -e CI=1 \
  -e PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
  "$image" \
  node_modules/.bin/playwright test --project=webkit "$@"
