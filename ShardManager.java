package id.cookieshop;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/** Mata uang "Shards" sederhana, disimpan di plugins/CookieShop/shards.yml */
public class ShardManager {

    private final CookieShop plugin;
    private final File file;
    private final YamlConfiguration data;
    private boolean dirty;

    public ShardManager(CookieShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shards.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public double get(UUID id) {
        return data.getDouble("players." + id, 0);
    }

    public void set(UUID id, double amount) {
        data.set("players." + id, Math.max(0, amount));
        dirty = true;
    }

    public void give(UUID id, double amount) {
        set(id, get(id) + amount);
    }

    public boolean take(UUID id, double amount) {
        double cur = get(id);
        if (cur + 1e-9 < amount) return false;
        set(id, cur - amount);
        return true;
    }

    public void save() {
        if (!dirty) return;
        try {
            data.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan shards.yml: " + e.getMessage());
        }
    }
}
