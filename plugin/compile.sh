#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
if [[ ! -e api/spigot.jar ]]; then
	cat <<EOF

ERROR: api/spigot.jar is missing!

This repository does not bundle with it the Minecraft source code (for obvious
legal reasons).

Most plugins only require the Spigot API - however, this plugin uses direct
calls into the Minecraft code (also called NMS) to be able to give loot from
loot tables. That means you will need the full Spigot jar as an API reference
to be able to compile this plugin. Place your copy of the Spigot jar file in
api/spigot.jar and re-run this script.

EOF
	exit 1
fi

ant pmd build jar
retcode=$?
if [[ $retcode -ne 0 ]]; then
	exit $retcode
fi
