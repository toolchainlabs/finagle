#!/bin/bash

set -eu

util_build_file="3rdparty/jvm/com/twitter/BUILD.v2"
cat << EOF > "${util_build_file}"
UTIL_VERSION = 'TODO'

def util_lib(name):
    artifact_name = f'util-{name}_2.13'
    jvm_artifact(
      name=artifact_name,
      group='com.twitter',
      artifact=artifact_name,
      version=UTIL_VERSION,
      packages=[f'com.twitter.{name}.**'],
    )

EOF

for n in $(rg -o 'util\("[^)]*"\)' build.sbt | sort -u | awk -F\" '{print $2 }'); do
  echo "util_lib('${n}')" >> "${util_build_file}"
done

for t in $(rg --glob='BUILD' -I -o '3rdparty/jvm/[^"]*' | sort -u); do
  dir=$(echo "$t" | awk -F: '{print $1}')
  build_file_name="${dir}/BUILD.v2"
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
