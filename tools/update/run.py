#!/usr/bin/env python3
"""Runs the Scripted Quests upgrade Command Line Interface."""

import argparse
from pathlib import Path
from update_lib import all_updaters


def main():
    """The main Command Line Interface (CLI) for upgrading multiple Scripted Quests files at once"""
    arg_parser = argparse.ArgumentParser(description=__doc__)
    arg_parser.add_argument(
        'old_version',
        choices=all_updaters.keys(),
        help="The version you wish to upgrade from"
    )
    arg_parser.add_argument(
        'scripted_quests_path',
        type=Path,
        nargs='+',
        help="A Scripted Quests plugin folder"
    )
    args = arg_parser.parse_args()

    print(f'Upgrading from version {args.old_version}')
    updaters = {}
    for old_version, updater in all_updaters.items():
        if len(updaters) != 0 or args.old_version in updaters:
            updaters[old_version] = updater

    for root in args.scripted_quests_path:
        print(f'- {root}')
        for updater in all_updaters.values():
            updater(root)


if __name__ == '__main__':
    main()
