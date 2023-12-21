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

        self.walk(root, 'zone_property_groups', self.update_zone_property_group_file)

        zone_layers = root / "zone_layers"
        zone_namespaces = root / "zone_namespaces"
        if zone_namespaces.exists():
            print(f'    - Found zone_namespaces, assuming already upgraded.')
        elif not zone_layers.exists():
            print('    - Did not find zone_layers, creating an empty zone_namespaces folder.')
            try:
                zone_namespaces.mkdir(mode=0o755, parents=True, exist_ok=True)
            except Exception:
                print('      - Failed!')
        else:
            print('    - Renaming zone_layers dir to zone_namespaces')
            try:
                zone_layers.rename(zone_namespaces)
            except Exception:
                print('      - Failed!')


    def update_zone_property_file(self, path):
        """Updates a Zone Property file from 6.x to 7.0."""

        short_path = self.shorten_path(path)

        print(f'      - Updating zone property file {short_path}...', end='')

        old_data = None
        try:
            with path.open('r', encoding='utf-8-sig') as fp:
                old_data = json.load(fp)
        except Exception:
            print('Could not read file!')
            return

        new_data = {}

        namespace = old_data.get("layer", None)
        if not isinstance(namespace, str):
            if "namespace" in old_data:
                print("Already updated")
                return
            print('Could not get "layer" field!')
            return
        new_data["namespace"] = namespace

        name = old_data.get("name", None)
        if not isinstance(name, str):
            print('Could not get "name" field!')
            return
        new_data["name"] = name

        display_name = old_data.get("display_name", None)
        if isinstance(display_name, str):
            new_data["display_name"] = display_name

        quest_components = old_data.get("quest_components", None)
        if type(quest_components) != list:
            print('Could not get "quest_components" field!')
            return
        new_data["quest_components"] = quest_components

        events = old_data.get("events", None)
        if events is not None:
            new_data["events"] = events

        try:
            with path.open('w', encoding='utf-8') as fp:
                json.dump(
                    new_data,
                    fp,
                    ensure_ascii=False,
                    indent=2,
                    separators=(',', ': '),
                    sort_keys=False
                )
                fp.write("\n")

            print("Done.")
        except Exception:
            print('Could not overwrite file!')
            return


    def update_zone_property_group_file(self, path):
        """Updates a Zone Property Group file from 6.x to 7.0."""

        short_path = self.shorten_path(path)

        print(f'      - Updating zone property file {short_path}...', end='')

        old_data = None
        try:
            with path.open('r', encoding='utf-8-sig') as fp:
                old_data = json.load(fp)
        except Exception:
            print('Could not read file!')
            return

        new_data = {}

        namespace = old_data.get("layer", None)
        if not isinstance(namespace, str):
            if "namespace" in old_data:
                print("Already updated")
                return
            print('Could not get "layer" field!')
            return
        new_data["namespace"] = namespace

        name = old_data.get("name", None)
        if not isinstance(name, str):
            print('Could not get "name" field!')
            return
        new_data["name"] = name

        properties = old_data.get("properties", None)
        if type(properties) != list:
            print('Could not get "properties" field!')
            return
        new_data["properties"] = properties

        try:
            with path.open('w', encoding='utf-8') as fp:
                json.dump(
                    new_data,
                    fp,
                    ensure_ascii=False,
                    indent=2,
                    separators=(',', ': '),
                    sort_keys=False
                )
                fp.write("\n")

            print("Done.")
        except Exception:
            print('Could not overwrite file!')
            return


all_updaters["6.x"] = UpdaterSixDotX


if __name__ == '__main__':
    print("This is not the main updater program. Please run run.py instead.")
