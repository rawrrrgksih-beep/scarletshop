package id.cookieshop;

import id.cookieshop.ShopManager.Category;
import id.cookieshop.ShopManager.Currency;
import id.cookieshop.ShopManager.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShopMenu implements Listener {

    private final CookieShop plugin;

    public ShopMenu(CookieShop plugin) {
        this.plugin = plugin;
    }

    /** Penanda inventory milik plugin ini. category == null berarti menu utama. */
    private static class Holder implements InventoryHolder {
        final Category category;
        final Map<Integer, ShopItem> items = new HashMap<>();
        final Map<Integer, Category> links = new HashMap<>();
        int backSlot = -1;
        Inventory inventory;

        Holder(Category category) {
            this.category = category;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    // =====================================================================
    //  Open menus
    // =====================================================================

    public void openMain(Player p) {
        int rows = Math.max(1, Math.min(6, plugin.getConfig().getInt("main-menu.rows", 6)));
        Holder h = new Holder(null);
        h.inventory = Bukkit.createInventory(h, rows * 9,
                plugin.color(plugin.getConfig().getString("main-menu.title", "&8Shop")));

        for (Category c : plugin.manager().all()) {
            if (c.menuSlot < 0 || c.menuSlot >= rows * 9) continue;

            ItemStack icon = new ItemStack(c.icon);
            ItemMeta m = icon.getItemMeta();
            if (m != null) {
                m.setDisplayName(plugin.color(c.name));
                List<String> lore = new ArrayList<>();
                for (String l : c.lore) lore.add(plugin.color(l));
                if (!lore.isEmpty()) lore.add("");
                lore.add(plugin.line("lore.open"));
                m.setLore(lore);
                m.addItemFlags(ItemFlag.values());
                icon.setItemMeta(m);
            }
            h.inventory.setItem(c.menuSlot, icon);
            h.links.put(c.menuSlot, c);
        }
        p.openInventory(h.inventory);
    }

    public void openCategory(Player p, Category c) {
        int size = c.rows * 9;
        Holder h = new Holder(c);
        h.inventory = Bukkit.createInventory(h, size, plugin.color(c.title));

        // Tombol Back (pojok kiri bawah)
        h.backSlot = size - 9;
        h.inventory.setItem(h.backSlot, backButton());

        // 1) item dengan slot manual
        List<ShopItem> auto = new ArrayList<>();
        for (ShopItem it : c.items) {
            int s = it.slot();
            if (s >= 0 && s < size && s != h.backSlot && !h.items.containsKey(s)) {
                put(h, c, s, it);
            } else {
                auto.add(it);
            }
        }

        // 2) sisanya: auto layout kolom 1-7 tiap baris
        int cursor = 0;
        for (ShopItem it : auto) {
            while (cursor < size && (cursor % 9 == 0 || cursor % 9 == 8 || h.items.containsKey(cursor))) {
                cursor++;
            }
            if (cursor >= size) {
                plugin.getLogger().warning("Kategori '" + c.id + "' terlalu penuh, beberapa item tidak ditampilkan.");
                break;
            }
            put(h, c, cursor, it);
            cursor++;
        }

        p.openInventory(h.inventory);
    }

    private void put(Holder h, Category c, int slot, ShopItem it) {
        h.items.put(slot, it);
        h.inventory.setItem(slot, display(c, it));
    }

    private ItemStack backButton() {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("back-button.material", "RED_STAINED_GLASS_PANE"));
        ItemStack is = new ItemStack(mat != null ? mat : Material.RED_STAINED_GLASS_PANE);
        ItemMeta m = is.getItemMeta();
        if (m != null) {
            m.setDisplayName(plugin.color(plugin.getConfig().getString("back-button.name", "&cBack")));
            is.setItemMeta(m);
        }
        return is;
    }

    private ItemStack display(Category c, ShopItem it) {
        ItemStack is = new ItemStack(it.material(), Math.min(it.amount(), it.material().getMaxStackSize()));
        ItemMeta m = is.getItemMeta();
        if (m == null) return is;

        if (it.name() != null) m.setDisplayName(plugin.color(it.name()));

        List<String> lore = new ArrayList<>();
        for (String l : it.lore()) lore.add(plugin.color(l));
        if (!lore.isEmpty()) lore.add("");

        if (it.buy() >= 0) {
            if (it.isCommand()) {
                lore.add(plugin.line("lore.buy-command", "price", plugin.price(c.currency, it.buy())));
            } else {
                lore.add(plugin.line("lore.buy",
                        "amount", String.valueOf(it.amount()),
                        "price", plugin.price(c.currency, it.buy())));
                int bundles = stackBundles(it);
                if (bundles > 1) {
                    lore.add(plugin.line("lore.buy-stack",
                            "amount", String.valueOf(bundles * it.amount()),
                            "price", plugin.price(c.currency, it.buy() * bundles)));
                }
            }
        }
        if (it.sell() >= 0 && !it.isCommand()) {
            lore.add(plugin.line("lore.sell",
                    "amount", String.valueOf(it.amount()),
                    "price", plugin.price(c.currency, it.sell())));
            lore.add(plugin.line("lore.sell-all"));
        }

        m.setLore(lore);
        m.addItemFlags(ItemFlag.values());
        is.setItemMeta(m);
        return is;
    }

    /** Jumlah bundle untuk mengisi 1 stack penuh (Shift + klik kiri). */
    private int stackBundles(ShopItem it) {
        return Math.max(1, it.material().getMaxStackSize() / it.amount());
    }

    // =====================================================================
    //  Events
    // =====================================================================

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);

        if (e.getClickedInventory() != e.getInventory()) return; // klik di inventory pemain
        if (!(e.getWhoClicked() instanceof Player p)) return;

        int slot = e.getRawSlot();

        // Menu utama -> buka kategori
        if (h.category == null) {
            Category c = h.links.get(slot);
            if (c != null) {
                click(p);
                openCategory(p, c);
            }
            return;
        }

        // Tombol Back
        if (slot == h.backSlot) {
            click(p);
            openMain(p);
            return;
        }

        ShopItem it = h.items.get(slot);
        if (it == null) return;

        Category c = h.category;
        if (c.currency == Currency.MONEY && plugin.economy() == null) {
            p.sendMessage(plugin.msg("economy-missing"));
            fail(p);
            return;
        }

        ClickType t = e.getClick();
        switch (t) {
            case LEFT -> buy(p, c, it, 1);
            case SHIFT_LEFT -> buy(p, c, it, stackBundles(it));
            case RIGHT -> sell(p, c, it, false);
            case SHIFT_RIGHT -> sell(p, c, it, true);
            default -> { }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    // =====================================================================
    //  Buy / Sell
    // =====================================================================

    private void buy(Player p, Category c, ShopItem it, int bundles) {
        if (it.buy() < 0) {
            p.sendMessage(plugin.msg("not-for-sale"));
            fail(p);
            return;
        }
        if (it.isCommand()) bundles = 1;

        double price = it.buy() * bundles;
        int total = it.amount() * bundles;

        if (!plugin.has(p, c.currency, price)) {
            p.sendMessage(plugin.msg("not-enough",
                    "price", plugin.price(c.currency, price),
                    "balance", plugin.price(c.currency, plugin.balance(p, c.currency))));
            fail(p);
            return;
        }
        if (!it.isCommand() && !hasSpace(p, new ItemStack(it.material()), total)) {
            p.sendMessage(plugin.msg("no-space"));
            fail(p);
            return;
        }
        if (!plugin.withdraw(p, c.currency, price)) {
            fail(p);
            return;
        }

        if (it.isCommand()) {
            for (String cmd : it.commands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
            }
        } else {
            p.getInventory().addItem(new ItemStack(it.material(), total));
        }

        p.sendMessage(plugin.msg("bought",
                "amount", String.valueOf(total),
                "item", prettyName(it),
                "price", plugin.price(c.currency, price)));
        ok(p);
    }

    private void sell(Player p, Category c, ShopItem it, boolean all) {
        if (it.sell() < 0 || it.isCommand()) {
            p.sendMessage(plugin.msg("not-sellable"));
            fail(p);
            return;
        }

        ItemStack proto = new ItemStack(it.material());
        int have = count(p, proto);
        int bundles = all ? have / it.amount() : 1;
        int total = bundles * it.amount();

        if (bundles < 1 || have < total) {
            p.sendMessage(plugin.msg("not-enough-items", "item", prettyName(it)));
            fail(p);
            return;
        }

        double price = it.sell() * bundles;
        remove(p, proto, total);
        plugin.deposit(p, c.currency, price);

        p.sendMessage(plugin.msg("sold",
                "amount", String.valueOf(total),
                "item", prettyName(it),
                "price", plugin.price(c.currency, price)));
        ok(p);
    }

    // =====================================================================
    //  Inventory helpers
    // =====================================================================

    private boolean hasSpace(Player p, ItemStack proto, int total) {
        int max = proto.getMaxStackSize();
        int space = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s == null || s.getType().isAir()) {
                space += max;
            } else if (s.isSimilar(proto)) {
                space += Math.max(0, max - s.getAmount());
            }
            if (space >= total) return true;
        }
        return false;
    }

    private int count(Player p, ItemStack proto) {
        int n = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s != null && s.isSimilar(proto)) n += s.getAmount();
        }
        return n;
    }

    private void remove(Player p, ItemStack proto, int amount) {
        ItemStack[] contents = p.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && amount > 0; i++) {
            ItemStack s = contents[i];
            if (s == null || !s.isSimilar(proto)) continue;
            int take = Math.min(amount, s.getAmount());
            s.setAmount(s.getAmount() - take);
            if (s.getAmount() <= 0) contents[i] = null;
            amount -= take;
        }
        p.getInventory().setStorageContents(contents);
    }

    private String prettyName(ShopItem it) {
        if (it.name() != null) return ChatColor.stripColor(plugin.color(it.name()));
        StringBuilder sb = new StringBuilder();
        for (String w : it.material().name().toLowerCase(Locale.ROOT).split("_")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    // ---------- sounds ----------

    private void click(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
    }

    private void ok(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
    }

    private void fail(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
    }
}
