package com.leathershorts.dumper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.leathershorts.dumper.scrapers.*;
import com.leathershorts.dumper.utils.DumperIO;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Dumper.MOD_ID)
public class Dumper {
    public Path outDir = null;
    public static final String MOD_ID = "dumper";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = Dumper.getLogger(null);

    // Flags
    private volatile boolean scrapedClient = false;
    private volatile boolean scrapedServer = false;
    private boolean quitRequested = false;
    private boolean worldCreationStarted = false;

    // Command-line flags
    private final boolean noZip = Boolean.getBoolean("nozip");
    private final boolean noAuto = Boolean.getBoolean("noauto");
    private final boolean noQuit = Boolean.getBoolean("noquit");

    // Message interval timer
    private int messageTimer = 0;


    /** Entrypoint of all dumper operations */
    public Dumper() {
        LOGGER.info("noAuto: {}", this.noAuto);
        LOGGER.info("noauto property: {}", System.getProperty("noauto"));
        LOGGER.info("noZip: {}", this.noZip);
        LOGGER.info("nozip property: {}", System.getProperty("nozip"));
        LOGGER.info("noQuit: {}", this.noQuit);
        LOGGER.info("noquit property: {}", System.getProperty("noquit"));

        // Initialise the dumper directory
        try {
            outDir = Files.createTempDirectory("dumper-");
            LOGGER.info("Output directory for dumper: {}", outDir.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to create output directory", e);
            quit(e);
            return;
        }

        LOGGER.info("Starting up scraping of modpack...");

        // The server scraper, where most of the scraping takes place
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);

        // The client scraper, where the scraping of the assets folder takes place
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.addListener(this::onClientTick));
    }


    // Dumper event listeners
    /** The scraping logic for the server side. Put everything that doesn't have to be client side in here */
    private void onServerStarted(ServerStartedEvent event) {
        if (quitRequested) return;

        FileSystemProvider.installedProviders().forEach(provider -> {
            if (provider.getScheme().equals("jar")) {
                LOGGER.info("Provider class: {}", provider.getClass().getName());
                LOGGER.info("Provider loader: {}", provider.getClass().getClassLoader());
            }
        });

        // Main extraction block for server side
        try {
            try (@Nullable InputStream manifest = Dumper.class.getResourceAsStream("/dumper_manifest.json")) {
                if (manifest == null) {
                    throw new IOException("dumper_manifest.json not found");
                }

                FileUtils.copyInputStreamToFile(
                    manifest,
                    outDir.resolve("dumper_manifest.json").toFile()
                );
            }


            DumperIO dio = DumperIO.create(this.outDir);
            dio.writeJson("mods.json", ModScraper.scrape());

            // Create and populate the data directory
            ResourceScraper scraper = new ResourceScraper(ResourceScraper.Side.SERVER, outDir.resolve("data"));
            scraper.scrape("recipes");
            scraper.scrape("loot_tables");
            scraper.scrape("advancements");

            // Populate the biomes registry differently
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            Registry<Biome> biomes = server.registryAccess().registryOrThrow(Registries.BIOME);

            // Populate the root with the rest
            dio
                .writeJson("blocks.json", RegistryScraper.scrape(ForgeRegistries.BLOCKS))
                .writeJson("items.json", RegistryScraper.scrape(ForgeRegistries.ITEMS))
                .writeJson("entities.json", RegistryScraper.scrape(ForgeRegistries.ENTITY_TYPES))
                .writeJson("effects.json", RegistryScraper.scrape(ForgeRegistries.MOB_EFFECTS))
                .writeJson("enchantments.json", RegistryScraper.scrape(ForgeRegistries.ENCHANTMENTS))
                .writeJson("fluids.json", RegistryScraper.scrape(ForgeRegistries.FLUIDS))
                .writeJson("biomes.json", RegistryScraper.scrape(biomes));

            // Finish up
            LOGGER.info("Created files from scraping");
            scrapedServer = true;
        } catch (IOException | NullPointerException e) {
            LOGGER.error("Failed to scrape modpack", e);
            quit(e);
        }
    }

    /** The client scraping logic. Put everything that can't be server side in here */
    private void onClientTick(TickEvent.ClientTickEvent event) {
        // Make sure that the player is in a world that has loaded
        if (quitRequested || event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        if (!scrapedClient || !scrapedServer) {
            if (mc.player == null || mc.level == null) {
                if (!this.noAuto && !this.worldCreationStarted) {
                    this.worldCreationStarted = true;

                    LevelSettings settings = new LevelSettings(
                        "Dumper",
                        GameType.CREATIVE,
                        false,
                        Difficulty.NORMAL,
                        true,
                        new GameRules(),
                        WorldDataConfiguration.DEFAULT
                    );

                    mc.createWorldOpenFlows().createFreshLevel(
                        "Dumper",
                        settings,
                        WorldOptions.defaultWithRandomSeed(),
                        registryAccess -> registryAccess.registry(Registries.WORLD_PRESET)
                            .flatMap(registry -> registry.getOptional(WorldPresets.FLAT))
                            .map(WorldPreset::createWorldDimensions)
                            .orElseGet(() -> WorldPresets.createNormalWorldDimensions(registryAccess))
                    );
                }
                return;
            }

            if (++messageTimer >= 100) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("Dumper is still scraping and dumping..."),
                    false
                );

                messageTimer = 0;
            }
        }

        // Quit if the server and client have scraped
        if (scrapedClient && scrapedServer) {
            LOGGER.info("Finished scraping minecraft modpack. Creating specified output format...");

            // If the user doesn't want a zip, copy the dir over
            if (this.noZip) {
                try {
                    Path dest = Path.of("dumper"); // DO NOT MAKE THIS BLANK. Will clear working dir otherwise
                    FileUtils.deleteDirectory(dest.toFile());
                    FileUtils.moveDirectory(outDir.toFile(), dest.toFile());
                } catch (IOException e) {
                    LOGGER.error("Failed to move dumper directory.", e);
                    quit(e);
                    return;
                }
            } else {
                try {
                    LOGGER.info("Creating dumper.zip");
                    DumperIO.zipOutput(this.outDir);
                    LOGGER.info("Created dumper.zip");
                } catch (IOException e) {
                    LOGGER.error("Failed to create dumper.zip", e);
                    quit(e);
                    return;
                } finally {
                    try {
                        LOGGER.info("got here");
                        FileUtils.deleteDirectory(outDir.toFile());
                    } catch (IOException e) {
                        LOGGER.error("Failed to delete temporary directory", e);
                        quit(e);
                    }
                }
            }

            quit();
            return;
        }

        // Main extraction block for client side
        if (scrapedClient) return; // Protect client from multiple scrapes
        try {
            // Create and populate the assets directory
            ResourceScraper scraper = new ResourceScraper(ResourceScraper.Side.CLIENT, outDir.resolve("assets"));
            scraper.scrape("models");
            scraper.scrape("blockstates");
            scraper.scrape("lang");
            scraper.scrape("textures");

            scrapedClient = true;
        } catch (IOException e) {
            LOGGER.error("Failed to scrape client assets", e);
            quit(e);
        }
    }


    // Utility methods
    /**
     * Quits minecraft safely. Use when dumper has finished dumping or a
     * critical error in scraping/dumping is encountered
     */
    private void quit() {
        this.quit(null);
    }

    /**
     * Quits minecraft safely. Use when dumper has finished dumping or a
     * critical error in scraping/dumping is encountered
     */
    private void quit(@Nullable Exception error) {
        if (quitRequested) return;
        quitRequested = true;
        LOGGER.warn("Requesting quit of minecraft. Minecraft will close or crash soon");

        if (this.noQuit) return;

        if (error != null) {
            Minecraft.getInstance().emergencySave();
            Minecraft.getInstance().delayCrash(new CrashReport("Dumper failed", error));
            return;
        }
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
    public static Logger getLogger(@Nullable Class<?> clazz) {
        return LoggerFactory.getLogger(
            MOD_ID.substring(0, 1).toUpperCase() +
                MOD_ID.substring(1) +
                (clazz == null ? "" : "/" + clazz.getSimpleName())
        );
    }
}
