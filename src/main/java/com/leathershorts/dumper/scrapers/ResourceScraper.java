package com.leathershorts.dumper.scrapers;

import com.google.gson.*;
import com.leathershorts.dumper.Dumper;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.slf4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public final class ResourceScraper {
    private static final Logger LOGGER = Dumper.getLogger(ResourceScraper.class);

    private ResourceScraper() {}

    public static Map<String, JsonElement> scrape(Side side, String... folders) throws IOException {
        Map<String, JsonElement> files = new HashMap<>();

        ResourceManager manager = switch (side) {
            case CLIENT -> DistExecutor.unsafeCallWhenOn(
                Dist.CLIENT,
                () -> () -> Minecraft.getInstance().getResourceManager()
            );
            case SERVER -> Minecraft.getInstance().getResourceManager();
        };

        ResourceManagerScraper scraper = new ResourceManagerScraper(manager, LOGGER);

        for (String folder : folders) {
            JsonElement extracted = scraper.scrape(folder);
            files.put(folder, extracted);
        }

        return files;
    }

    public enum Side {
        CLIENT, SERVER
    }
}
