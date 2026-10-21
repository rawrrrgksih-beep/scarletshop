package id.cookieshop;

import id.cookieshop.ShopManager.Currency;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CookieShop extends JavaPlugin {

    private Economy economy;
    private ShardManager shards;
    private ShopManager manager;
    private ShopMenu menu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "shop.yml").exists()) {
            saveResource("shop.yml", false);
        }

        shards = new ShardManager(this);
        manager = new ShopManager(this);
        manager.load();
        menu = new ShopMenu(this);
        getServer().getPluginManager().registerEvents(menu, this);

        ShopCommand shopCmd = new ShopCommand(this);
        for (String name : new String[]{"shop", "shardshop"}) {
            getCommand(name).setExecutor(shopCmd);
            getCommand(name).setTabCompleter(shopCmd);
        }
        ShardsCommand shardsCmd = new ShardsCommand(this);
        getCommand("shards").setExecutor(shardsCmd);
        getCommand("shards").setTabCompleter(shardsCmd);

        // Cek economy setelah semua plugin selesai load
        getServer().getScheduler().runTask(this, () -> {
            if (economy() == null) {
                getLogger().warning("Tidak ada economy provider (Vault + EssentialsX/CMI/dll). Kategori bermata uang 'money' tidak akan bisa dipakai.");
            }
        });
        // Auto-save shards tiap 5 menit
        getServer().getScheduler().runTaskTimer(this, () -> shards.save(), 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        if (shards != null) shards.save();
    }

    public void reloadAll() {
        reloadConfig();
        manager.load();
    }

    // ---------- accessors ----------

    public ShardManager shards() { return shards; }
    public ShopManager manager() { return manager; }
    public ShopMenu menu() { return menu; }

    public Economy economy() {
        if (economy == null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }
        return economy;
    }

    // ---------- currency ----------

    public double balance(Player p, Currency c) {
        if (c == Currency.SHARDS) return shards.get(p.getUniqueId());
        Economy e = economy();
        return e == null ? 0 : e.getBalance(p);
    }

    public boolean has(Player p, Currency c, double amount) {
        return balance(p, c) + 1e-9 >= amount;
    }

    public boolean withdraw(Player p, Currency c, double amount) {
        if (c == Currency.SHARDS) return shards.take(p.getUniqueId(), amount);
        Economy e = economy();
        return e != null && e.withdrawPlayer(p, amount).transactionSuccess();
    }

    public void deposit(Player p, Currency c, double amount) {
        if (c == Currency.SHARDS) {
            shards.give(p.getUniqueId(), amount);
        } else {
            Economy e = economy();
            if (e != null) e.depositPlayer(p, amount);
        }
    }

    // ---------- text helpers ----------

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    /** Pesan chat dengan prefix. kv = pasangan placeholder, nilai. */
    public String msg(String key, String... kv) {
        String m = getConfig().getString("messages." + key, "&cMissing message: " + key);
        return color(getConfig().getString("prefix", "") + replace(m, kv));
    }

    /** Baris lore / teks dari config tanpa prefix. */
    public String line(String path, String... kv) {
        return color(replace(getConfig().getString(path, path), kv));
    }

    private String replace(String text, String... kv) {
        for (int i = 0; i + 1 < kv.length; i += 2) {
            text = text.replace("%" + kv[i] + "%", kv[i + 1]);
        }
        return text;
    }

    public String price(Currency c, double amount) {
        String f = getConfig().getString("currency." + c.name().toLowerCase(Locale.ROOT), "%s");
        return f.replace("%s", compact(amount));
    }

    /** 153560 -> 153.56K */
    public String compact(double v) {
        double a = Math.abs(v);
        if (a >= 1_000_000_000d) return trim(v / 1_000_000_000d) + "B";
        if (a >= 1_000_000d) return trim(v / 1_000_000d) + "M";
        if (a >= 1_000d) return trim(v / 1_000d) + "K";
        return trim(v);
    }

    private String trim(double d) {
        return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US)).format(d);
    }
}
