#!/bin/bash
set -eux

# Run `tailor`.
./pants_from_sources tailor

# And regenerate the lockfile, which will determine what to include via the current implementation
# of inference, and so will frequently need updating.
./pants_from_sources coursier-resolve
