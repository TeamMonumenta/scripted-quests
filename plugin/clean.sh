#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ant clean
retcode=$?
if [[ $retcode -ne 0 ]]; then
	exit $retcode
fi
