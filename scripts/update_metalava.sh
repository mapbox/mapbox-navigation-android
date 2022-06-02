#!/bin/bash

set -e

echo -n "Cloning…"

if [ ! -d ./build/fake-aosp/tools/metalava ]; then
    git clone -q https://android.googlesource.com/platform/tools/metalava/ ./build/fake-aosp/tools/metalava
fi

(
    cd ./build/fake-aosp/tools/metalava

    # Update in case the repo was already cloned.
    git pull -q
    echo " Done"

    echo -n "Building…"
    # Dev branch has JAVA_HOME defined in the project, if building from development branches,
    # uncomment below to exclude JAVA_HOME export and use JAVA_HOME from local machine.
    sed '/^export JAVA_HOME=/d' gradlew > gradlew.temp
    mv gradlew.temp gradlew
    chmod a+x gradlew

    # Pick the release tag you prefer,
    # more detailed tags/versions can be found at https://android.googlesource.com/platform/tools/metalava/+refs
    # git checkout android-12.1.0_r5
    ./gradlew jar --console=plain -q --no-daemon
    find ../../out/metalava/libs ! -name '*full*' -type f -exec cp {} ../../../../metalava/metalava.jar \;
    echo " Done"

    echo -e "\nDependencies:\n"
    ./gradlew dependencies --no-daemon --configuration implementation | \egrep '^.--- ' | cut -d' ' -f2
)
