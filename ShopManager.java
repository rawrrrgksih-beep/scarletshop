package id.cookieshop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ShopManager {

    public enum Currency { MONEY, SHARDS }

    /** buy/sell = harga per "bundle" (sejumlah amount). Nilai < 0 berarti tidak bisa dibeli/dijual. */
    public record ShopItem(Material material, int amount, double buy, double sell,
                           String name, List<String> lore, List<String> commands, int slot) {
        public boolean isCommand() {
            return !commands.isEmpty();
        }
    }

    public static class Category {
        public final String id;
        public String name;
        public String title;
        public Material icon = Material.CHEST;
        public List<String> lore = new ArrayList<>();
        public Currency currency = Currency.MONEY;
        public int rows = 3;
        public int menuSlot = -1;
        public final List<ShopItem> items = new ArrayList<>();

        public Category(String id) {
            this.id = id;
        }
    }

    private final CookieShop plugin;
    private final Map<String, Category> categories = new LinkedHashMap<>();

    public ShopManager(CookieShop plugin) {
        this.plugin = plugin;
    }

    public Collection<Category> all() {
        return categories.values();
    }

    public Set<String> ids() {
        return categories.keySet();
    }

    public Category get(String id) {
        return id == null ? null : categories.get(id.toLowerCase(Locale.ROOT));
    }

    public void load() {
        categories.clear();
        File file = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("categories");
        if (root == null) {
            plugin.getLogger().warning("shop.yml: section 'categories' tidak ditemukan.");
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;

            Category c = new Category(id.toLowerCase(Locale.ROOT));
            c.name = s.getString("name", "&a" + id);
            c.title = s.getString("title", "&8Shop - " + id);
            Material icon = Material.matchMaterial(s.getString("icon", "CHEST"));
            c.icon = icon != null ? icon : Material.CHEST;
            c.lore = s.getStringList("lore");
            c.menuSlot = s.getInt("menu-slot", -1);
            c.currency = "shards".equalsIgnoreCase(s.getString("currency", "money"))
                    ? Currency.SHARDS : Currency.MONEY;

            ConfigurationSection items = s.getConfigurationSection("items");
            if (items != null) {
                for (String key : items.getKeys(false)) {
                    ConfigurationSection i = items.getConfigurationSection(key);
                    if (i == null) continue;

                    String matName = i.getString("material", key);
                    Material m = Material.matchMaterial(matName);
                    if (m == null || m.isAir() || !m.isItem()) {
                        plugin.getLogger().warning("shop.yml: material tidak valid '" + matName
                                + "' di kategori '" + id + "' (dilewati).");
                        continue;
                    }
                    c.items.add(new ShopItem(
                            m,
                            Math.max(1, i.getInt("amount", 1)),
                            i.getDouble("buy", -1),
                            i.getDouble("sell", -1),
                            i.getString("name"),
                            i.getStringList("lore"),
                            i.getStringList("commands"),
                            i.getInt("slot", -1)));
                }
            }

            int autoRows = (int) Math.ceil(c.items.size() / 7.0);
            c.rows = Math.max(1, Math.min(6, s.getInt("rows", Math.max(3, autoRows))));
            categories.put(c.id, c);
        }
        plugin.getLogger().info("Loaded " + categories.size() + " kategori shop.");
    }
}
