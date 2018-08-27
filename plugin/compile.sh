#!/bin/bash

# Update version number
perl -p -i -e "s|<version>dev</version>|<version>$(git describe --tags --always --dirty)</version>|g" pom.xml

cat <<EOF
NOTE: To build this project, you will likely also need to build spigot 1.12.2
on the same system. This will place the spigot jar someplace Maven can find
it so that this build will work too.
EOF

mvn clean install
retcode=$?
if [[ $retcode -ne 0 ]]; then
	exit $retcode
fi
