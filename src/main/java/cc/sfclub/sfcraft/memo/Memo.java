package cc.sfclub.sfcraft.memo;

import cc.sfclub.sfcraft.memo.config.MemoConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.SneakyThrows;
import net.fabricmc.api.ModInitializer;

import java.nio.file.Files;
import java.nio.file.Path;

public class Memo implements ModInitializer {
    private static final Path CONFIG_ROOT = Path.of("./config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    @Getter
    private static Memo instance;
    @Getter
    private MemoConfig config;

    @Override
    public void onInitialize() {
        instance = this;
        config = loadConfig();
    }

    @SneakyThrows
    private MemoConfig loadConfig() {
        var _cfg = CONFIG_ROOT.resolve("memo.json");
        if (Files.notExists(_cfg)) {
            var cfg = new MemoConfig();
            Files.createDirectory(CONFIG_ROOT);
            Files.writeString(_cfg, GSON.toJson(cfg));
            return cfg;
        }
        return GSON.fromJson(Files.readString(_cfg), MemoConfig.class);
    }
}
