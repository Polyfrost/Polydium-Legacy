package org.polyfrost.polydium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PolydiumConfig {
    private static final Logger LOGGER = LogManager.getLogger("Polydium");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("polydium.json");

    public static final PolydiumConfig INSTANCE = load();

    public boolean particleCollision = true;
    public boolean entityBlockCaching = true;
    public boolean hudCaching = true;
    public int hudFps = 60;
    public boolean connectSrvLookup = true;
    public boolean preciseFrameLimiter = true;
    public boolean worldBorderCaching = true;
    public boolean tickCulling = true;
    public boolean hologramFastPath = true;
    public boolean entityShadowCache = true;

    private static PolydiumConfig load() {
        PolydiumConfig config = null;
        boolean failed = false;
        if (Files.isRegularFile(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                config = GSON.fromJson(reader, PolydiumConfig.class);
            } catch (IOException | JsonParseException exception) {
                LOGGER.warn("Could not read Polydium config, using defaults without saving", exception);
                failed = true;
            }
        }
        if (config == null) {
            config = new PolydiumConfig();
        }
        if (!failed) {
            config.save();
        }
        return config;
    }

    private void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save Polydium config", exception);
        }
    }
}
