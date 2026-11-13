package com.leathershorts.dumper.scrapers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leathershorts.dumper.Dumper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.slf4j.Logger;

public final class ModScraper {
    private static final Logger LOGGER = Dumper.getLogger(ModScraper.class);

    private ModScraper() {}

    public static JsonElement scrape() {
        JsonArray topLevel = new JsonArray();

        for (IModInfo mod : ModList.get().getMods()) {
            LOGGER.info("Scraping info for mod: {}", mod.getModId());

            // Build the base for the mod's info object
            JsonObject json = new JsonObject();
            json.addProperty("id", mod.getModId());
            json.addProperty("version", mod.getVersion().toString());
            json.addProperty("description", mod.getDescription());
            json.addProperty("name", mod.getDisplayName());

            // Build the dependencies object array
            JsonArray dependencies = new JsonArray();
            for (IModInfo.ModVersion dependency : mod.getDependencies()) {
                JsonObject dep = new JsonObject();
                dep.addProperty("id", dependency.getModId());
                dep.addProperty("version_range", dependency.getVersionRange().toString());
                dep.addProperty("required", dependency.isMandatory());
                dep.addProperty("side", dependency.getSide().toString());
                dependencies.add(dep);
            }
            json.add("dependencies", dependencies);

            // Add to the main array in the file
            topLevel.add(json);
            LOGGER.info("Scraped data from {}", mod.getModId());
            // Maybe add mod properties in the future

            LOGGER.info("Finished scraping info from mod: {}", mod.getModId());
        }

        // Pass the json object back to the code
        return topLevel;
    }
}
