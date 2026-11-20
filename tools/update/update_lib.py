#!/usr/bin/env python3
"""A library of update methods. This should not be run directly."""

from __future__ import annotations

import json
import os
from dataclasses import dataclass
from pathlib import Path


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




@dataclass(frozen=True)
class PropertyKey:
    """Identifies one zone property: the shard it is authored for, its namespace and its name."""

    shard: str
    namespace: str
    name: str

    @property
    def is_exit(self):
        """True for the "!Name" file that only carries the exit behavior of another property."""

        return self.name.startswith('!')

    @property
    def without_exit_marker(self):
        """The property an exit file belongs to."""

        return PropertyKey(self.shard, self.namespace, self.name[1:])


@dataclass
class PropertyFile:
    """One zone property file and the behavior gathered for it during the migration."""

    key: PropertyKey
    path: Path
    data: dict
    dirty: bool = False

    def merge(self, other):
        """Appends the sections of another file that declares the same property."""

        for section in ('quest_components', 'on_enter', 'on_exit', 'on_death', 'on_login'):
            entries = other.get(section)
            if isinstance(entries, list) and entries:
                self.data.setdefault(section, []).extend(entries)

        events = other.get('events')
        if isinstance(events, dict):
            merged = self.data.setdefault('events', {})
            for event_type, entries in events.items():
                if isinstance(entries, list) and entries:
                    merged.setdefault(event_type, []).extend(entries)

        if 'display_name' not in self.data and 'display_name' in other:
            self.data['display_name'] = other['display_name']

        self.dirty = True

    def add(self, section, entries):
        """Appends entries to one behavior section, ignoring an empty list."""

        if isinstance(entries, list) and entries:
            self.data.setdefault(section, []).extend(entries)
            self.dirty = True

    def add_rule(self, section, entry):
        """Appends one death or login rule. Returns False when the rule is already there."""

        entries = self.data.setdefault(section, [])
        if entry in entries:
            return False

        entries.append(entry)
        self.dirty = True
        return True

    def render(self):
        """Returns the file content with the sections in their canonical order."""

        rendered = {'namespace': self.data['namespace'], 'name': self.data['name']}

        if 'display_name' in self.data:
            rendered['display_name'] = self.data['display_name']
        if 'on_enter' in self.data:
            rendered['on_enter'] = self.data['on_enter']

        for section in ('on_exit', 'on_death', 'on_login'):
            if self.data.get(section):
                rendered[section] = self.data[section]

        if self.data.get('events'):
            rendered['events'] = self.data['events']

        return rendered


@dataclass
class Rule:
    """One death or login rule read from the old per-shard folders."""

    path: Path
    data: dict
    section: str
    priority: int
    target: PropertyKey | None

    def entry(self):
        """The rule as it appears inside a behavior file."""

        return {'priority': self.priority, **self.data}


class UpdaterEightDotX(Updater):
    """The Scripted Quests 8.x to 9.0 updater class.

    Consolidates everything one zone property does into a single file: the exit behavior
    that used to live in a "!"-named file becomes on_exit, and the death/login rules that
    targeted the property become on_death/on_login. Rules that cannot move into a property
    file stay in the death or login folder, with a priority added.
    """

    def __init__(self, root):
        """Updates the selected path from one ScriptedQuests version to 9.0."""

        super().__init__(root)

        print(f'  - Updating {root} from 8.x to 9.0...')

        self.warnings = []
        self.properties = {}

        self.walk(root, 'zone_properties', self.index_property)
        self.fold_exit_files()
        self.rename_enter_sections()
        self.migrate_rules('death', 'on_death')
        self.migrate_rules('login', 'on_login')
        self.write_properties()
        self.report()

    ######################################## helpers ########################################

    def warn(self, message):
        """Records a warning to print once at the end of the run."""

        self.warnings.append(message)

    def read_json(self, path):
        """Reads one JSON file, returning None and warning if it cannot be parsed."""

        try:
            with path.open('r', encoding='utf-8-sig') as fp:
                return json.load(fp)
        except Exception:
            self.warn(f'{self.shorten_path(path)}: could not read file')
            return None

    def write_json(self, path, data):
        """Writes one JSON file with the same formatting the other updaters use."""

        with path.open('w', encoding='utf-8') as fp:
            json.dump(
                data,
                fp,
                ensure_ascii=False,
                indent=2,
                separators=(',', ': '),
                sort_keys=False
            )
            fp.write("\n")

    def shard_of(self, path, source):
        """Returns the shard folder a content file belongs to."""

        parts = path.relative_to(source).parts
        return parts[0] if len(parts) > 1 else 'common'

    def property_shard(self, path):
        """Returns the shard folder a zone property file belongs to."""

        parts = path.relative_to(self.root / 'zone_properties').parts
        return parts[0] if len(parts) > 1 else 'common'

    ######################################## zone properties ########################################

    def key_of(self, path, data):
        """Returns the key a zone property file declares, or None when the file is invalid."""

        namespace = data.get('namespace')
        name = data.get('name')
        if not isinstance(namespace, str) or not isinstance(name, str):
            return None

        return PropertyKey(self.property_shard(path), namespace, name)

    def index_property(self, path):
        """Adds one zone property file to the index, merging files that declare the same property."""

        data = self.read_json(path)
        if data is None:
            return

        key = self.key_of(path, data)
        if key is None:
            self.warn(f'{self.shorten_path(path)}: missing namespace or name, skipped')
            return

        existing = self.properties.get(key)
        if existing is None:
            self.properties[key] = PropertyFile(key, path, data)
            return

        self.warn(f'{self.shorten_path(path)}: duplicate of {self.shorten_path(existing.path)}, merged')
        existing.merge(data)
        path.unlink()

    def fold_exit_files(self):
        """Moves every "!"-named property file into the on_exit of the property it belongs to."""

        for key in [key for key in self.properties if key.is_exit]:
            exit_file = self.properties.pop(key)
            base = self.base_property(exit_file)

            if exit_file.data.get('events'):
                self.warn(f'{self.shorten_path(exit_file.path)}: events in an exit file are never used, dropped')

            base.add('on_exit', exit_file.data.get('quest_components'))
            if 'display_name' in exit_file.data and 'display_name' not in base.data:
                base.data['display_name'] = exit_file.data['display_name']

            base.dirty = True
            exit_file.path.unlink()

    def base_property(self, exit_file):
        """Returns the property an exit file belongs to, creating an empty one when absent."""

        key = exit_file.key.without_exit_marker
        base = self.properties.get(key)
        if base is not None:
            return base

        path = exit_file.path.parent / f'{key.name}.json'
        base = PropertyFile(key, path, {'namespace': key.namespace, 'name': key.name}, dirty=True)
        self.properties[key] = base
        print(f'      - Created {self.shorten_path(path)} for an exit-only property')
        return base

    def rename_enter_sections(self):
        """Renames quest_components to on_enter, keeping the old components first."""

        for property_file in self.properties.values():
            if 'quest_components' not in property_file.data:
                continue

            components = property_file.data.pop('quest_components')
            on_enter = property_file.data.setdefault('on_enter', [])
            property_file.data['on_enter'] = list(components) + on_enter
            property_file.dirty = True

    ######################################## death and login rules ########################################

    def migrate_rules(self, folder, section):
        """Moves every rule that targets a property file, and leaves the rest in place."""

        source = self.root / folder
        if not source.is_dir():
            print(f'    - Could not find {folder} folder, skipping!')
            return

        rules = self.collect_rules(source, section)
        moved = 0
        for rule in rules:
            if self.route(rule):
                rule.path.unlink()
                moved += 1
            else:
                self.write_json(rule.path, rule.entry())

        print(f'      - Moved {moved} of {len(rules)} {folder} rules, left {len(rules) - moved} in place')

        for leftover in sorted(source.rglob('*')):
            if leftover.is_file() and leftover.suffix != '.json':
                self.warn(f'{self.shorten_path(leftover)}: not a .json file, left in place')

    def collect_rules(self, source, section):
        """Reads every rule of one kind, ranked by the order the old loader gave it."""

        ranks = self.rule_ranks(source)
        unranked = len(ranks)
        rules = []

        for path in sorted(source.glob('**/*.json')):
            data = self.read_json(path)
            if data is None:
                continue

            shard = self.shard_of(path, source)
            rules.append(Rule(
                path=path,
                data=data,
                section=section,
                priority=ranks.get(path, unranked),
                target=self.first_positive_target(path, data, shard),
            ))

        return rules

    def rule_ranks(self, source):
        """Ranks every rule by the position the old loader gave it.

        The old loader read one flat list sorted by full path, with the common folder mixed
        in alphabetically. Common rules now always run first, so they keep the leading ranks.
        """

        common_dir = source / 'common'
        common = sorted(common_dir.glob('**/*.json')) if common_dir.is_dir() else []
        ranks = {path: index for index, path in enumerate(common)}

        shards = sorted({
            path.relative_to(source).parts[0]
            for path in source.glob('**/*.json')
            if len(path.relative_to(source).parts) > 1
        })

        for shard in shards:
            if shard == 'common':
                continue
            shard_files = sorted((source / shard).glob('**/*.json'))
            for index, path in enumerate(shard_files, start=len(common)):
                ranks[path] = index

        return ranks

    def first_positive_target(self, path, data, shard):
        """Returns the first property a rule requires, ignoring negated entries."""

        prerequisites = data.get('prerequisites')
        if not isinstance(prerequisites, dict):
            return None

        zone_properties = prerequisites.get('zone_properties')
        if not isinstance(zone_properties, dict):
            return None

        target = None
        negated = False
        for namespace, names in zone_properties.items():
            if not isinstance(names, list):
                continue
            for name in names:
                if not isinstance(name, str):
                    continue
                if name.startswith('!'):
                    negated = True
                elif target is None:
                    target = PropertyKey(shard, namespace, name)

        if negated:
            self.warn(f'{self.shorten_path(path)}: keeps a negated property, moved verbatim')

        return target

    def route(self, rule):
        """Appends the rule to the file of the property it targets. Returns False when it cannot move.

        A rule only moves into a property file of its own shard: a rule from a shard folder
        must not become common, or it would load on every shard.
        """

        if rule.target is None:
            return False

        property_file = self.properties.get(rule.target)
        if property_file is None:
            return False

        if property_file.add_rule(rule.section, rule.entry()):
            return True

        self.warn(f'{self.shorten_path(property_file.path)}: {rule.section} rule already present, skipped')
        return True

    ######################################## output ########################################

    def write_properties(self):
        """Writes every zone property file that changed."""

        written = 0
        for property_file in self.properties.values():
            if not property_file.dirty:
                continue

            property_file.path.parent.mkdir(parents=True, exist_ok=True)
            self.write_json(property_file.path, property_file.render())
            written += 1

        print(f'      - Wrote {written} zone property files')

    def report(self):
        """Prints the warnings collected during the run."""

        if not self.warnings:
            return

        print(f'    - {len(self.warnings)} warning(s):')
        for warning in self.warnings:
            print(f'      - {warning}')


all_updaters["6.x"] = UpdaterSixDotX
all_updaters["8.x"] = UpdaterEightDotX


if __name__ == '__main__':
    print("This is not the main updater program. Please run run.py instead.")
