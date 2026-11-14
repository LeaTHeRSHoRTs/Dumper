package com.leathershorts.dumper.scrapers;

import com.google.gson.*;
import com.leathershorts.dumper.Dumper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

public final class ResourceScraper {
    private static final Logger LOGGER = Dumper.getLogger(ResourceScraper.class);
    private final ResourceManager manager;
    private final Path masterFolder;

    public ResourceScraper(Side side, Path masterFolder) throws IOException {
        FileUtils.forceMkdir(masterFolder.toFile());

        this.manager = switch (side) {
            case CLIENT -> DistExecutor.unsafeCallWhenOn(
                Dist.CLIENT,
                () -> () -> Minecraft.getInstance().getResourceManager()
            );
            case SERVER -> ServerLifecycleHooks.getCurrentServer().getResourceManager();
        };

        this.masterFolder = masterFolder;
    }

    /**
     * Scrapes the specified folder and immediately puts the resources into `output`
     * @param folder The folder to scrape
     * @param predicate The predicate used to decide which resources should be included and excluded from the subset
     * @throws IOException When the copy operation fails
     */
    public void scrape(String folder, Predicate<ResourceLocation> predicate) throws IOException {
        Map<ResourceLocation, Resource> resources =
            this.manager.listResources(folder, predicate);

        for (var entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            Resource resource = entry.getValue();

            Path destination = this.masterFolder.resolve(id.getNamespace()).resolve(id.getPath());

            try (InputStream input = resource.open()) {
                FileUtils.copyInputStreamToFile(input, destination.toFile());
            }
        }

        LOGGER.info(
            "Successfully scraped {} resources from {} into {}",
            resources.size(), folder, this.masterFolder
        );
    }

    /**
     * Scrapes the specified folder and immediately puts the resources into `output`
     * @param folder The folder to scrape
     * @throws IOException When the copy operation fails
     */
    public void scrape(String folder) throws IOException {
        this.scrape(folder, _u -> true);
    }

    public enum Side {
        CLIENT, SERVER
    }
}
