# Dumper

Dumper is a minecraft mod that is used in conjunction with MineBook to scrape data from minecraft.

## Usage

Install dumper in your forge modpack, and then launch the client. Dumper will handle the rest!
For usage fo flags, see the flags section

> [!NOTE]
> Use the "-all" version of the modpack if your system does not come bundled with the Apache Commons IO Jar

### Steps
Dumper will go through 5 steps to scrape and collect data from your game
1. Scrape data folder
2. Open a new world via world creation
3. Scrape assets folder (this must be done by creating a new world and opening it)
4. Zips up the folder minecraft/dumper, putting all the contents in dumper.zip 
5. Quits the game

You only need to open minecraft, and dumper will handle all the scraping, closing minecraft when it is finished. It's all automatic!

### Resulting Content
The dumper ZIP file consists of many different JSON files. They include:
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

### Flags
Flags must be passed as JVM arguments
These particular flags affect dumper in various ways

- #### No Zip
  This flag causes the output to be contained in a `dumper/` directory instead of a `dumper.zip` file

  Flag: `-Dnozip`
- #### No Auto
  This flag causes the program to enter minecraft, but not create a new world.

  Flag: `-Dnoauto`
- #### No Quit
  This flag stops the program from quitting minecraft when running. Errors from dumper will not cause the program to crash in this mode

  Flag: `-Dnoquit`

## Changelog
### 1.0.0
 - Initial release
### 1.1.0
 - Changed to producing zip files instead of a dumper folder
 - Changed to use Apache Commons IO as well as the native Java IO
### 2.0.0
 - Renamed the generated `manifest.json` file to `dumper_manifest.json` to avoid naming conflicts
 - Added the `-Dnozip` flag to output a directory instead of a ZIP file
 - Added the `-Dnoquit` flag to prevent Dumper from automatically closing minecraft when it has finished scraping
 - Added the `-Dnoauto` flag to prevent Dumper from automatically creating and opening a world
 - Refined Dumper's I/O operations behind the scenes