package id.cookieshop;

import id.cookieshop.ShopManager.Category;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Menangani /shop [kategori|reload] dan /shardshop */
public class ShopCommand implements TabExecutor {

    private final CookieShop plugin;

    public ShopCommand(CookieShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        boolean isShop = cmd.getName().equalsIgnoreCase("shop");

        if (isShop && a.length > 0 && a[0].equalsIgnoreCase("reload")) {
            if (!s.hasPermission("cookieshop.admin")) {
                s.sendMessage(plugin.msg("no-permission"));
                return true;
            }
            plugin.reloadAll();
            s.sendMessage(plugin.msg("reloaded"));
            return true;
        }

        if (!(s instanceof Player p)) {
            s.sendMessage(plugin.msg("only-players"));
            return true;
        }
        if (!p.hasPermission("cookieshop.use")) {
            p.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (!isShop) { // /shardshop
            open(p, "shards");
            return true;
        }
        if (a.length == 0) {
            plugin.menu().openMain(p);
        } else {
            open(p, a[0]);
        }
        return true;
    }

    private void open(Player p, String id) {
        Category c = plugin.manager().get(id);
        if (c == null) {
            p.sendMessage(plugin.msg("shop-not-found"));
        } else {
            plugin.menu().openCategory(p, c);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        if (!cmd.getName().equalsIgnoreCase("shop") || a.length != 1) return out;

        String prefix = a[0].toLowerCase(Locale.ROOT);
        for (String id : plugin.manager().ids()) {
            if (id.startsWith(prefix)) out.add(id);
        }
        if (s.hasPermission("cookieshop.admin") && "reload".startsWith(prefix)) out.add("reload");
        return out;
    }
}
