package com.leathershorts.dumper.utils;

import com.google.gson.JsonElement;
import com.leathershorts.dumper.Dumper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class DumperIO {
    private static final Logger LOGGER = Dumper.getLogger(DumperIO.class);
    private final Path outDir;

    private DumperIO(Path outDir) {
        this.outDir = outDir;
    }

    public static DumperIO create(Path outDir) {
        return new DumperIO(outDir);
    }

    public DumperIO writeJson(String output, JsonElement data) throws IOException {
        FileUtils.writeStringToFile(
            outDir.resolve(output).toFile(),
            Dumper.GSON.toJson(data),
            StandardCharsets.UTF_8
        );
        LOGGER.info("Created file {}", output);

        return this;
    }

    public static void zipOutput(Path output) throws IOException {
        Path zip = Path.of("dumper.zip");

        try (
            ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip));
            Stream<Path> paths = Files.walk(output)
        ) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    zos.putNextEntry(new ZipEntry(output.relativize(file).toString().replace('\\', '/')));
                    Files.copy(file, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
