package com.leathershorts.dumper.scrapers;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

public final class ResourceManagerScraper {
    private final ResourceManager manager;
    private final Logger logger;
    public ResourceManagerScraper(ResourceManager manager, Logger logger) {
        this.manager = manager;
        this.logger = logger;
    }

    public JsonElement scrape(String folder) throws IOException {
        Map<ResourceLocation, List<Resource>> resources =  this.manager.listResourceStacks(folder, _u -> true);

        JsonArray scraped = new JsonArray();
        for (var entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            List<Resource> stack = entry.getValue();

            for (Resource resource : stack) {
                try (Reader reader = new InputStreamReader(resource.open())) {
                    JsonObject item = new JsonObject();
                    item.addProperty("id", id.toString());
                    item.addProperty("pack", resource.sourcePackId());

                    try {
                        item.add("data", JsonParser.parseReader(reader));
                    } catch (JsonParseException e) {
                        logger.warn("Could not parse file {}. Invalid json. Skipping...", id);
                        continue;
                    }
                    scraped.add(item);
                }
            }
        }

        logger.info("Successfully scraped from {}", folder);
        return scraped;
    }
}
