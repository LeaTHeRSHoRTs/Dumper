package com.leathershorts.dumper;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.leathershorts.dumper.scrapers.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Dumper.MOD_ID)
public class Dumper {
    public final Path OUT_DIR = Path.of("dumper");
    public static final String MOD_ID = "dumper";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Flags
    private static volatile boolean scrapedClient = false;
    private static volatile boolean scrapedServer = false;
    private static boolean quitRequested = false;

    /**
     * Entrypoint of all dumper operations
     */
    public Dumper() {
        // Initialise the dumper directory
        try {
            Files.createDirectories(OUT_DIR);
            LOGGER.info("Output directory for dumper: {}", OUT_DIR.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to create output directory", e);
            quit();
            return;
        }

        LOGGER.info("Starting up scraping of modpack...");

        // The server scraper, where most of the scraping takes place
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);

        // The client scraper, where the scraping of the assets folder takes place
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.addListener(this::onClientTick));
    }

    /**
     * The scraping logic for the server side. Put everything that doesn't have to be client side in here
     */
    private void onServerStarted(ServerStartedEvent event) {
        FileSystemProvider.installedProviders().forEach(provider -> {
            if (provider.getScheme().equals("jar")) {
                LOGGER.info("Provider class: {}", provider.getClass().getName());
                LOGGER.info("Provider loader: {}", provider.getClass().getClassLoader());
            }
        });

        // Main extraction block for server side
        try {
            writeToFile("mods.json", ModScraper.scrape());

            // Create and populate the data directory
            Files.createDirectories(OUT_DIR.resolve("data"));
            ResourceScraper scraper = new ResourceScraper(ResourceScraper.Side.SERVER, OUT_DIR.resolve("data"));
            scraper.scrape("recipes");
            scraper.scrape("loot_tables");
            scraper.scrape("advancements");

            // Populate the root with the rest
            writeToFile("blocks.json", RegistryScraper.scrape(ForgeRegistries.BLOCKS));
            writeToFile("items.json", RegistryScraper.scrape(ForgeRegistries.ITEMS));
            writeToFile("entities.json", RegistryScraper.scrape(ForgeRegistries.ENTITY_TYPES));
            writeToFile("effects.json", RegistryScraper.scrape(ForgeRegistries.MOB_EFFECTS));
            writeToFile("enchantments.json", RegistryScraper.scrape(ForgeRegistries.ENCHANTMENTS));
            writeToFile("fluids.json", RegistryScraper.scrape(ForgeRegistries.FLUIDS));

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

            Registry<Biome> biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
            writeToFile("biomes.json", RegistryScraper.scrape(biomes));

            LOGGER.info("Created files from scraping");

            scrapedServer = true;
        } catch (IOException e) {
            LOGGER.error("Failed to scrape modpack", e);
            quit();
        }
    }

    /**
     * The client scraping logic. Put everything that can't be server side in here
     */
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Make sure that the player is in a world that has loaded
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().level == null) return;

        // Quit if the server and client have scraped
        if (scrapedClient) {
            if (scrapedServer) quit();
            return;
        }

        // Main extraction block for client side
        try {
            // Create and populate the assets directory
            Files.createDirectories(OUT_DIR.resolve("assets"));
            ResourceScraper scraper = new ResourceScraper(ResourceScraper.Side.CLIENT, OUT_DIR.resolve("assets"));
            scraper.scrape("models");
            scraper.scrape("blockstates");
            scraper.scrape("lang");
            scraper.scrape("textures");

            scrapedClient = true;
            if (scrapedServer) quit();
        } catch (IOException e) {
            LOGGER.error("Failed to scrape client assets", e);
            quit();
        }
    }

    private void writeToFile(String output, JsonElement data) throws IOException {
        Files.createDirectories(this.OUT_DIR.resolve(output).getParent());

        try (Writer writer = Files.newBufferedWriter(this.OUT_DIR.resolve(output))) {
            new GsonBuilder().setPrettyPrinting().create().toJson(data, writer);
            LOGGER.info("Created file {}", output);
        } catch (IOException e) {
            throw new IOException("Failed to create file " + output, e);
        }
    }

    /**
     * Quits minecraft safely. Use when dumper has finished dumping or a
     * critical error in scraping/dumping is encountered
     */
    private static void quit() {
        if (quitRequested) return;
        quitRequested = true;

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().stop());
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) server.halt(false);
        });
    }

    /**
     * A logger creation helper for the classes used in dumper
     * @param clazz The class that is the parent of the logger being created
     * @return A logger that is to be used for the current class
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(MOD_ID + "/" + clazz.getSimpleName());
    }
}
