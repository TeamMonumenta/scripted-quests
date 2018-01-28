#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ant pmd build jar
retcode=$?
if [[ $retcode -ne 0 ]]; then
	exit $retcode
fi
