# Dumper

Dumper is a minecraft mod that is used in conjunction with MineBook to scrape data from minecraft

## Usage

Install dumper in your forge modpack, and then launch the client. Once you are in the game, enter a world and let dumper do its thing.

> [!NOTE]
> Use the "-all" version of the modpack if your system does not come bundled with the Apache Commons IO Jar

### Steps
Dumper will go through 4 steps to scrape and collect data from your game
1. Scrape data folder
2. Scrape assets folder (this must be done by creating a new world and opening it)
3. Zips up the folder minecraft/dumper, putting all the contents in dumper.zip
4. Quits the game

### Resulting Content
The dumper zip file consists of many different JSON files. They include:
- dumper_manifest.json _(For dumper related data)_
- biomes.json
- blocks.json
- effects.json
- enchantments.json
- entities.json
- fluids.json
- items.json
- mods.json
- assets folder
  - blockstates.json
  - lang.json
  - models.json
- data folder
  - advancements.json
  - loot_tables.json
  - recipes.json

## Changelog
### 1.0.0
 - Initial release
### 1.1.0
 - Changed to producing zip files instead of a dumper folder
 - Changed to use Apache Commons IO as well as the native Java IO
### 2.0.0
 - Changed the name of the manifest.json file generated to dumper_manifest.json to prevent ambiguities
 - Added the ability to force dumper to not produce a zip and instead preserve the dumper directory when quitting (When using the -Dnozip flag)