package id.cookieshop;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /shards                       -> lihat shards sendiri
 * /shards <player>              -> lihat shards pemain lain
 * /shards give|take|set <player> <jumlah>   (cookieshop.admin)
 */
public class ShardsCommand implements TabExecutor {

    private final CookieShop plugin;

    public ShardsCommand(CookieShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        ShardManager sm = plugin.shards();

        if (a.length == 0) {
            if (!(s instanceof Player p)) {
                s.sendMessage(plugin.msg("only-players"));
                return true;
            }
            s.sendMessage(plugin.msg("shards-balance",
                    "amount", plugin.compact(sm.get(p.getUniqueId()))));
            return true;
        }

        String sub = a[0].toLowerCase(Locale.ROOT);
        if (sub.equals("give") || sub.equals("take") || sub.equals("set")) {
            if (!s.hasPermission("cookieshop.admin")) {
                s.sendMessage(plugin.msg("no-permission"));
                return true;
            }
            if (a.length < 3) {
                s.sendMessage(plugin.msg("shards-usage"));
                return true;
            }
            OfflinePlayer target = find(a[1]);
            if (target == null) {
                s.sendMessage(plugin.msg("player-not-found", "player", a[1]));
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(a[2]);
            } catch (NumberFormatException ex) {
                s.sendMessage(plugin.msg("invalid-amount"));
                return true;
            }
            if (amount < 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
                s.sendMessage(plugin.msg("invalid-amount"));
                return true;
            }

            switch (sub) {
                case "give" -> sm.give(target.getUniqueId(), amount);
                case "take" -> sm.set(target.getUniqueId(), sm.get(target.getUniqueId()) - amount);
                default -> sm.set(target.getUniqueId(), amount);
            }
            s.sendMessage(plugin.msg("shards-updated",
                    "player", target.getName() == null ? a[1] : target.getName(),
                    "amount", plugin.compact(sm.get(target.getUniqueId()))));
            return true;
        }

        // /shards <player>
        OfflinePlayer target = find(a[0]);
        if (target == null) {
            s.sendMessage(plugin.msg("player-not-found", "player", a[0]));
            return true;
        }
        s.sendMessage(plugin.msg("shards-other",
                "player", target.getName() == null ? a[0] : target.getName(),
                "amount", plugin.compact(sm.get(target.getUniqueId()))));
        return true;
    }

    private OfflinePlayer find(String name) {
        OfflinePlayer op = Bukkit.getPlayerExact(name);
        if (op == null) op = Bukkit.getOfflinePlayerIfCached(name);
        return op;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            if (s.hasPermission("cookieshop.admin")) {
                for (String x : new String[]{"give", "take", "set"}) {
                    if (x.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(x);
                }
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(p.getName());
            }
        } else if (a.length == 2 && s.hasPermission("cookieshop.admin")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(a[1].toLowerCase(Locale.ROOT))) out.add(p.getName());
            }
        }
        return out;
    }
}
