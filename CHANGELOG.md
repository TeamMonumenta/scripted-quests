# Changelog

### 1.14
- Interactables feature - trigger quest components when a player clicks with an item
- Added several prereqs:
```
gamemode
item_in_either_hand
item_in_offhand
is_sneaking
is_fully_healed
only_one_of
```

### 1.13
#### Major
- Redeemable codes feature - a system to generate a code that is bound to a particular player, then redeem it to run actions
- Increase CommandAPI dependency to 1.8.1
- Player clickable actions feature - a system to let players run specific actions via typing `/clickable <label>`

#### Minor
- Timers spawned onto the same block as an identical timer remove the duplicate
- Timers that fail to detect the block under them as valid try again exactly once 10 ticks later
- /giveloottable now also takes an optional integer count
- Fix infinite bukkit runnable looping for restricted traders
- Allow formatting codes in NPC `dialog_text` messages
- NPCs can no longer be set on fire or have potion effects applied to them except instant health
- NPCs can't be interacted with via riptide
- Timer armor stands are always invulnerable
- There is now a "you moved too far away" error message for clickable text

### 1.12
- Added new Login Events system to run commands on players if they log out for some minimum period of time
- Timer armorstands are now invulnerable if their names are hidden

### 1.11
- There is a new prerequisite type `use_npc_for_prereqs` that lets you apply prerequisite checks to NPCs themselves.
- Only prerequisites that make sense in this context do anything - for example, inventory checking doesn't work.
- Fix trade restrictions not working correctly with nitwit villagers

### 1.10
I may have forgotten to release a new version for a long time.

**New race system:**
- Allows creation of "races" where a player can run through a series of waypoints to compete for high scores
- Races have leaderboards, stored in scoreboards
- Added `/race <start|stop|leaderboard>` to interact with races from in-game
- Added `/leaderboard <players> <objective> <descending> <page>` to show a leaderboard of player scores

**Trader Restrictions**
- Added a new mechanism to restrict trades of NPCs based on prerequisites

**Command Timers**
- Added an armorstand-based clock system to dramatically improve in-game clock performance.
- This eliminates the need to run repeating command blocks every tick to drive clocking systems.
- Timer stands are just armor stands sitting on top of impulse or repeating command blocks.
- Timers load and unload with chunks in the world, making them much more efficient than global clocks
- The plugin changes the commands underneath to "always active" or "needs redstone" to clock them on and off
- Added a /timerdebug command to get active timer information
- Added some global config settings to automatically show or hide all timer armorstand names
- This still needs documentation - for example, stand on an impulse command block that says /say hi and run this command:
```
/summon armor_stand ~ ~ ~ {Tags:["timer","period=20","radius=100"]}
```

**Other**
- Names are now automatically squashed down to labels containing only a-zA-Z0-9.
- Giving loot table contents now checks for full inventory and sends player a message about items being dropped
- Added 1.14-like improved `/schedule function` - 100% compatible with 1.14 version, but also supports re-scheduling same function multiple times
- Added `/randomnumber <min> <max>`, which can be used like `execute store result score randomnumber temp run randomnumber 1 5`
- Added `/haspermission <players> <permission>` to check if player has a particular permission
- Added `test_for_block` prerequisite to check for a block in the world
- Command actions are now pre-parsed to make sure they are valid commands
- Death actions run a tick after respawning, instead of before respawn
- Fix some weird interactions (#46) when having for example an NPC villager named Bob and an NPC wither skeleton named Fred and a non-NPC wither skeleton named Bob that could not be damaged.
- Added 10s cache for compass entries to prevent large numbers of entries from lagging the world

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
