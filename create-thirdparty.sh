#!/bin/bash

set -eu

for t in $(rg --glob='BUILD' -I -o '3rdparty/jvm/[^"]*' | sort -u); do
  dir=$(echo "$t" | awk -F: '{print $1}')
  build_file_name="${dir}/BUILD"
  group=$(echo "$dir" | cut -c 14- | tr '/' '.')

  target_name=$(echo "$t" | awk -F: '{print $2}')
  if [ -z "${target_name}" ]; then
    target_name=$(basename "$dir")
  fi

  mkdir -p "$dir"
  echo "jvm_artifact(" >> "${build_file_name}"
  echo "  name='${target_name}'," >> "${build_file_name}"
  echo "  group='${group}'," >> "${build_file_name}"
  echo "  artifact='${target_name}'," >> "${build_file_name}"
  echo "  version='TODO'," >> "${build_file_name}"
  echo ")" >> "${build_file_name}"
  echo >> "${build_file_name}"

done
