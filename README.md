# Dumper

Dumper is a minecraft mod that is used in conjunction with MineBook to scrape data from minecraft

## Usage

Install dumper in your forge modpack, and then launch the client. Once you are in the game, enter a world and let dumper do its thing.

### Steps
Dumper will go through 4 steps to scrape and collect data from your game
1. Scrape data folder
2. Scrape assets folder (this must be done by creating a new world and opening it)
3. Zips up the folder minecraft/dumper, putting all the contents in dumper.zip
4. Quits the game

### Resulting Content
The dumper zip file consists of many different JSON files. They include:
- manifest.json _(For dumper related data)_
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
