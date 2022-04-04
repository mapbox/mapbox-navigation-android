#!/bin/bash
set -eoux
# control sum of the key is const: echo | shasum -a 256 pgp_keys.asc
shaSumAscKey="d56942c32a1bb70af75bf972302b6114049fb59cb76193fac349bb9b587b60c2"

curl https://keybase.io/codecovsecurity/pgp_keys.asc -o pgp_keys.asc
# check sum
echo "$shaSumAscKey  pgp_keys.asc" | shasum -a 256 -c

gpg --no-default-keyring --keyring trustedkeys.gpg --import pgp_keys.asc

curl -Os https://uploader.codecov.io/latest/linux/codecov

curl -Os https://uploader.codecov.io/latest/linux/codecov.SHA256SUM

curl -Os https://uploader.codecov.io/latest/linux/codecov.SHA256SUM.sig

gpgv codecov.SHA256SUM.sig codecov.SHA256SUM

# check sum
shasum -a 256 -c codecov.SHA256SUM

chmod +x codecov
./codecov