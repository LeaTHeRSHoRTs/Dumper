package com.leathershorts.dumper.scrapers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leathershorts.dumper.Dumper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import org.slf4j.Logger;

import java.util.Set;


public final class RegistryScraper {
    private static final Logger LOGGER = Dumper.getLogger(RegistryScraper.class);

    private RegistryScraper() {}

    public static <T> JsonElement scrape(Registry<T> registry) {
        return iterate(registry.keySet());
    }

    public static <T> JsonElement scrape(IForgeRegistry<T> registry) {
        LOGGER.info("Scraping registry: {}", registry.getRegistryName());
        return iterate(registry.getKeys());
    }

    private static JsonElement iterate(Set<ResourceLocation> ids) {
        JsonArray arr = new JsonArray();

        for (ResourceLocation location : ids) {
            JsonObject object = new JsonObject();

            object.addProperty("id", location.toString());
            object.addProperty("namespace", location.getNamespace());
            object.addProperty("path", location.getPath());
            object.addProperty("translation_key", location.toLanguageKey());

            arr.add(object);
        }
        return arr;
    }
}
