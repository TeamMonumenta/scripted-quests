# Changelog

### 1.9
- Add an `interact_npc` action
- JSON files are now sorted alphanumerically before loading for consistent loading order across multiple files
- json-editor updated to maintained fork
- New `check_advancements` prerequisite

**Major changes to commands**
- Plugin now depends on CommandAPI > 1.4
- All commands except /questtrigger and /reloadquests now work with 1.13 syntax including via /execute and in functions
- These commands now all have a new player selector argument

### 1.8
- Update to 1.13.1

### 1.7
- Fix formatting bugs for long lines of `raw_text`
- Add @S replacement in clickable text
- Add `/giveloottable` command
- Refuse to damage quest NPCs
- All messages are now considered command messages, not chat messages
- Reloading is less verbose
- Compass now points to all death locations since last reload

### 1.6
- Add logical operator support to prerequisites

### 1.5
- Add a `give_loot` action to give loot from a loot table

### 1.4
- Remove all plugin metadata on /reload
- Now supports multiple delayed actions on same player

### 1.3
- Fix double-interactions with NPCs
- Added voice-over support to NPCs
- Enable multiple sequential `clickable_text` conversations
- More robust metadata clearing
- Add `item_in_hand` prerequisite

### 1.2
- Add "death quests" and supporting tools
- Add `/interactnpc` to trigger interactions
- Add `check_tags` prerequisite

### 1.1
- Rename /reloadconfig -> /reloadquests
- Compass locations now use the message as their title

### 1.0
- Initial release version
