#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# The sample fakes every status via `scene`, so no real system permission dialog is ever shown.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

install_sample
for mode in light dark; do
  set_night_mode "$mode"
  for scene in overview rationale denied; do
    fresh_launch --es scene "$scene"
    capture "$scene-$mode"
  done
done
