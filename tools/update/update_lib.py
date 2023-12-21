#!/usr/bin/env python3
"""A library of update methods. This should not be run directly."""

import json
import os


# Add entries in order from oldest to newest
# Insert order is preserved in Python 3.
all_updaters = {}


class Updater():
    """A Scripted Quests file updater."""

    def __init__(self, root):
        """Updates the selected path from one ScriptedQuests version to another."""

        self.root = root


    def walk(self, root, folder, callback, glob_pattern="**/*.json"):
        """Walks a folder within the Scripted Quests plugin folder.

        Runs a callback on all files matching a glob pattern (typically "**/*.json).
        """

        sub_dir = root / folder
        if not sub_dir.is_dir():
            print(f'    - Could not find {folder} folder, skipping!')
        else:
            print(f'    - Found {folder} folder, updating...')

            for child in sorted(sub_dir.glob(glob_pattern)):
                callback(child)


    def shorten_path(self, path):
        """Shortens a path relative to the root path for display purposes."""

        seps = os.sep if os.altsep is None else os.sep + os.altsep
        str_path = str(path)

        if self.root in path.parents:
            root = str(self.root)
            if any(root.endswith(sep) for sep in seps):
                return str_path[len(root):]
            return str_path[len(root) + 1:]
        return str_path


class UpdaterSixDotX(Updater):
    """The Scripted Quests 6.x to 7.0 updater class."""

    def __init__(self, root):
        """Updates the selected path from one ScriptedQuests version 6.x to 7.0."""

        super().__init__(root)

        print(f'  - Updating {root} from 6.x to 7.0...')

        self.walk(root, 'zone_properties', self.update_zone_property_file)

        """TODO:
        
        - Also walk through zone property groups
        - Move zone layer folder to zone namespace, populating "world_name" with ".*" in files
        """


    def update_zone_property_file(self, path):
        """Updates a Zone Properties file from 6.x to 7.0."""

        short_path = self.shorten_path(path)

        print(f'      - Updating zone properties file {short_path}...')

        """TODO:
        
        - Load json file to dict
        - Copy properties in priority order, swapping layer for namespace
          - Skip if layer not found and do not save
        - Save it back (there will always be changes)
        """


all_updaters["6.x"] = UpdaterSixDotX


if __name__ == '__main__':
    print("This is not the main updater program. Please run run.py instead.")
