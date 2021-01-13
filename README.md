# Scripted Quests
JSON-driven Minecraft Spigot plugin for creating quests

# Table of Contents

- [Description](#description)
- [Sample](#sample)
- [Download](#download)
- [Getting Started / Tutorial](#get-started)
- [Creating Quests with the Online Editor](#web-editor)
- [Structure of a Quests File](#structure)
- [List of Commands / Permissions](#commands)
- [Current Capabilities](#capabilities)

## <a name="description"></a>Description
This plugin is a Minecraft game developer's toolbox, greatly expanding and
simplifies what you can do with minecraft commands.

The idea is to be able to create complex mechanisms without having to write any
plugin code. With ScriptedQuests, you can build things like quest interactions
between players and NPCs (and much more) by creating configuration files with
the provided web editor.

Highlighted features:
- NPC interaction: Smack an NPC to talk to them
- Compass: Navigate around the world by cycling through compass objectives
- Death: Run special logic when players die under specific conditions
- Login: Run logic when players log in
- Races: Challenge players to achieve the fastest time along a path
- Traders: Restrict what villagers trade each player based on scores/rules
- Codes: Create a reward code in one map that can be redeemed in another map
- Clickables: Let non-op players run pre-bundled commands via chat
- Interactables: Create actions that run when players click with specific items
- Timers: Efficient command block clocks with optional player nearby detection
- Utility commands: `/randomnumber`, `/giveloottable`, `/schedule function`, `/set velocity`, `/getdate field`

This plugin was developed for use with Monumenta, a Minecraft
Complete-The-Monument MMORPG server. It has proven to be perhaps the most
generally valuable & reusable tool we have built over several years, and we
hope others will find it useful too.

## <a name="download"></a>Download

ScriptedQuests works on Minecraft 1.13 or higher. It also requires
[CommandAPI](https://github.com/JorelAli/1.13-Command-API) version 4.x. Other
CommandAPI versions may also work, worth giving it a shot. It'll be immediately
obvious as if the version is incompatible, the plugin won't load or no commands
will work.

You can download ScriptedQuests from [GitHub Packages](https://github.com/TeamMonumenta/scripted-quests/packages).
This is automatically updated every time new changes are made. Click on the
com.scriptedquests... link, and download the .jar file from the "Assets"
category on the right side of the page.

## <a name="sample"></a>Sample
Here is an example of interacting with a villager named Aimee:
![Example quest interaction](./samples/quest.png)

Quests are made up of two-way dialog - the NPC can "talk" to the player, and
the player can reply by clicking one of the available options in chat. At each
stage in the process arbitrary actions can occur, such as setting scoreboard
values, running commands/functions, sending more dialog, etc.

The quest compass is just a simple mapping between scoreboard values and
locations. For example, if you have score Quest01 = 1, left clicking with an
ordinary compass will make it point to some coordinates. For example:

![Example quest compass](./samples/compass.png)

Additonally, the compass will point to locations where you have died since the
server last restarted, and tells you how long ago those deaths were.

You can also create quests that involve the player dying in-game. These "death"
quest elements can trigger actions when the player dies if they meet specific
pre-requisites. This can involve keeping the player's inventory, respawning at
a different location than normal, etc.

## <a name="get-started"></a>Getting Started / Tutorial
This plugin only currently has compiled versions for Spigot 1.12.2. It will
probably work with older versions but you must compile it yourself.

- Install it like all spigot/paper plugins by placing it in your plugins folder.
- Start your server. This should create some folders under
  `plugins/ScriptedQuests/`
- Summon a test villager: `/summon minecraft:villager ~ ~ ~ {CustomName:"Aimee"}`
- Create the needed scoreboard: `/scoreboard objectives add Quest01 dummy`
- Install the quest & compass config files:
	- `samples/sample-quest.json` -> `plugins/ScriptedQuests/npc/sample-quest.json`
	- `samples/sample-compass.json` -> `plugins/ScriptedQuests/compass/sample-compass.json`
- Reload the quest configuration: `/reloadquests`
- Left-click Aimee to talk to her and start the quest.
- Give yourself a compass and right-click it to cycle available quests
- Give yourself the quest item: `/give @s minecraft:dye 1 11 {display:{Lore:["QuestItem"]}}`
- Talk to Aimee again to complete the quest

## <a name="web-editor"></a>Creating Quests with the Online Editor
Creating these by hand is a pain - use the web editor! The web editor not only
helps you structure things correctly but is also the primary documentation for
the various quest options

- Quest Editor - https://rawgit.com/TeamEpic/Scripted-Quests/master/tools/quest_editor.html
- Quest Compass Editor - https://rawgit.com/TeamEpic/Scripted-Quests/master/tools/quest_compass_editor.html
- Quest Death Editor - https://rawgit.com/TeamEpic/Scripted-Quests/master/tools/quest_death_editor.html
- Quest Login Editor - https://rawgit.com/TeamEpic/Scripted-Quests/master/tools/quest_login_editor.html
- Race Editor - https://rawgit.com/TeamEpic/Scripted-Quests/master/tools/race_editor.html
- Trader Editor - https://rawgit.com/TeamEpic/Scripted-Quests/master/tools/trader_editor.html
- Redeemable Codes Editor - https://rawgit.com/TeamEpic/Scripted-Quests/master/tools/code_editor.html
- Clickable Actions Editor - https://rawgit.com/TeamEpic/Scripted-Quests/master/tools/clickable_editor.html
- Interactable Items Editor - https://rawgit.com/TeamEpic/Scripted-Quests/master/tools/interactable_editor.html
- Zone Layers Editor - https://rawgit.com/TeamEpic/Scripted-Quests/master/tools/zone_layer_editor.html
- Zone Properties Editor - https://rawgit.com/TeamEpic/Scripted-Quests/master/tools/zone_property_editor.html

## <a name="structure"></a>Structure of a Quests File:
Each quest file has the following info:
- What NPC this quest is for (name, entity type)
- A list of quest components

Quest components contain optional pre-requisites and actions. If the
pre-requisites are met, the actions are run. Every time a player interacts with
an NPC, quest components are run in-order (more than one may be executed).

## <a name="commands"></a>List of Commands / Permissions
### Player commands

`/clickable <label>`
- permission: `scriptedquests.clickable`
- Runs the clickable with the specified label. This is useful for giving non-op players
  access to pre-prepared command bundles

`/interactNpc <npcName> [EntityType]`
`/interactNpc <npc>`
- permission: `scriptedquests.interactnpc`
- EntityType is optional (default = VILLAGER) and can be chosen from this
  list: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html
- When using the alternate syntax, npc is a selector, and acts identical to clicking that entity.
- This command can be used to build location - based quests that don't
  involve clicking on an NPC. To do this, use a repeating command block with a
  command of this form: `execute as @p[r = 5] run interactnpc Aimee VILLAGER`
- You will need to structure your QuestComponent so that the pre-requisites
  are only met once if the player will stay in that location for more than
  one invokation of interactnpc.
- Note that running this command every tick is likely unnecessary for most
  use cases - instead, attach it to a slow clock instead.

`/leaderboard <@a> <objective> <descending> <page>`
- permission: `scriptedquests.leaderboard`
- Shows the targeted players a leaderboard (sorted scoreboard) for the specified
  objective. `descending` specifies order of results, page specifies which page to show.
  **For players to be able to use the << >> arrows, they need to have this permission**.
  This of course also lets them generate leaderboards for any scoreboard on the server.

`/questTrigger`
- permission: `scriptedquests.questtrigger`
- **Players that interact with clickable text need to have this permission node.**
- This command is executed by players when they click on chat options.
- This command was designed for security. Players will not be able to use it to
  trigger dialog options except those presented by the NPC they are currently
  interacting with. They also will not be able to click on the same message
  twice.

`/race leaderboard <players> <raceLabel> <page>`
- permission: `scriptedquests.race`
- Send <players> page <page> of the leaderboard for <raceLabel>
### Quest developer commands

`/line <from> <to> <block>`
- permission: `scriptedquests.line`
- Sets a line of blocks between two locations, returning the number of blocks changed.

`/generatecode <@a> "<seed>"`
- permission: `scriptedquests.generatecode`
- Generates a unique three-word code for that player. This code will only work for that
  player and the seed specified. Redeemed with `/code`. This is useful for building
  cross-map functionality

`/execute store result score <scoreboardplayer> <objective> run getdate <field>`
- permission: `scriptedquests.getdate`
- Get part of the current date as a score. <field> can be:
  Year, Month, DayOfMonth, DayOfWeek, DaysSinceEpoch, SecondsSinceEpoch,
  IsPm, HourOfDay, HourOfTwelve, Minute, Second, or Ms.

`/giveloottable <@a> "<namespace:path/to/table>" [count]`
- permission: `scriptedquests.giveloottable`
- Gives all the items from the specified loot table to the specified players `count` times

`/haspermission <@a> <permission.node>`
- permission: `scriptedquests.haspermission`
- Utility function to tell the player that runs the command whether the target player(s)
  have the specified permission node or not. Also returns success/fail so you can use
  it with /execute store

`/execute store result score <scoreboardplayer> <objective> run randomnumber <min> <max>`
- permission: `scriptedquests.randomnumber`
- Used to store a random number into a scoreboard value. `min` and `max` are inclusive.

`/race start <players> <raceLabel>`
- permission: `scriptedquests.race`
- Start the race <raceLabel> for <players>

`/race stop <players>`
- permission: `scriptedquests.race`
- Stop <players> from participating in their current race

`/race win <players>`
- permission: `scriptedquests.race`
- Cause <players> to finish their current race; can be used for races that don't use rings

`/reloadQuests`
- permission: `scriptedquests.reloadquests`
- Reloads Quest NPCs and Quest Compass.
- Provides helpful debugging to resolve problems with any files that fail to load.
- Hover over the error messages in chat for more information.

`/schedule function <namespace:path/to/function> <ticks>`
- permission: `scriptedquests.schedulefunction`
- Schedules a function to run #ticks later. This is the exact same function provided in
  1.14+, except it also allows the same function to be scheduled more than once.

`/setvelocity <@a> <x> <y> <z>`
- permission: `scriptedquests.setvelocity`
- Sets the velocity of the targeted entity to x y z.

`/testzone <x> <y> <z> <layerName> [propertyName]`
- permission: `scriptedquests.testzone`
- Checks if the coordinates specified are in a zone of layerName, optionally with a
  given property name, which may be inverted with the prefix `!`. If layerName or
  propertyName have spaces, quotes, or other characters that may cause issues,
  put them in quotes similar to json.
### Debug commands

`/debugzones <player>`
`/debugzones <position>`
- permission: `scriptedquests.debugzones`
- Get debug information about which zone a player or position is.

`/timerdebug <enabledOnly>`
- permission: `scriptedquests.timerdebug`
- Shows a list of all the currently loaded timers. If `enabledOnly` is true, will only
  show timers that are currently active due to players matching the range criteria
## <a name="capabilities"></a>Current Capabilities:
prerequisites:
- check\_scores - Checks one or more scoreboard values for the player
- check\_tags - Checks if the player has (or must not have) specific scoreboard tags
- items\_in\_inventory - Checks that the player is carrying the specified items
- item\_in\_hand - Checks that the item in the player's main hand meets specified criteria
- location - Checks that the player is within an area
- and, or, not\_and, not\_or  - Allows building complex logical prerequisites.

By default, prerequisites are and'ed together - all must be true.

actions:
- dialog - Interaction with an NPC
	- text - Text spoken by the NPC
	- raw\_text - Text directly sent to the player
	- clickable\_text - Text that when clicked runs actions
- set\_scores - Sets scoreboard values for the player
- function - Runs a function using the console "as" the player (@s = player)
- command - Runs a command using the console
- give\_loot - Generates loot from a loot table and gives it to the player
- interact\_npc - Triggers a follow-on interaction with a different NPC
- rerun-components - Re-runs all components for this NPC from the beginning
