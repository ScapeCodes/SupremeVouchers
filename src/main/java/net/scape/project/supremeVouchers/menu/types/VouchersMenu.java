package net.scape.project.supremeVouchers.menu.types;

import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.NBTItem;
import net.scape.project.supremeVouchers.managers.VoucherManager;
import net.scape.project.supremeVouchers.menu.Menu;
import net.scape.project.supremeVouchers.menu.MenuUtil;
import net.scape.project.supremeVouchers.objects.Voucher;
import net.scape.project.supremeVouchers.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static net.scape.project.supremeVouchers.utils.Utils.color;
import static net.scape.project.supremeVouchers.utils.Utils.format;

public class VouchersMenu extends Menu {

    private final VoucherManager voucherManager;

    // Pagination
    private int page = 0;
    private final int maxPerPage = 54 - 18; // border rows = 9 top + 9 bottom

    private final int[] CONTENT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };

    public VouchersMenu(MenuUtil menuUtil, VoucherManager voucherManager) {
        super(menuUtil);
        this.voucherManager = voucherManager;
    }

    @Override
    public String getMenuName() {
        String s = menuUtil.getSearch();
        if (s == null || s.isEmpty()) return "Vouchers (Page " + (page + 1) + ")";
        return "Vouchers » Search: " + s + " (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {

        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        String name = ChatColor.stripColor(meta.getDisplayName());
        if (name == null || name.isBlank()) return;

        // ----- Pagination -----
        if (name.equalsIgnoreCase("next page")) {
            page++;
            super.refresh();
            return;
        }

        if (name.equalsIgnoreCase("previous page")) {
            page = Math.max(0, page - 1);
            super.refresh();
            return;
        }

        if (name.equalsIgnoreCase("search")) {

            p.closeInventory();
            p.sendMessage(format("&eType your search query. Type &c!clear &eto reset search."));
            p.sendMessage(format("&7Type &ccancel &7to stop."));

            menuUtil.awaitChatInput(input -> {

                if (input.equalsIgnoreCase("cancel")) {
                    p.sendMessage(format("&cSearch cancelled."));
                    Utils.runMainLater(this::open, 1);
                    return;
                }

                if (input.equalsIgnoreCase("!clear")) {
                    menuUtil.setSearch("");
                    p.sendMessage(format("&aSearch cleared."));
                    Utils.runMainLater(this::open, 1);
                    return;
                }

                menuUtil.setSearch(input);
                p.sendMessage(format("&aSearch applied: &f" + input));
                Utils.runMainLater(this::open, 1);
            });

            return;
        }


        // ----- Create -----
        if (name.equalsIgnoreCase("create new voucher")) {
            // your creation logic here if needed
            return;
        }

        // ----- Close -----
        if (name.equalsIgnoreCase("close")) {
            p.closeInventory();
            return;
        }

        // -------------------------------------
        // Handling voucher item clicks
        // -------------------------------------

        NBTItem nbti = new NBTItem(item);
        if (!nbti.hasKey("voucher_id")) return;

        String id = nbti.getString("voucher_id");
        Voucher voucher = voucherManager.get(id);
        if (voucher == null) return;

        ClickType type = e.getClick();

        // LEFT CLICK → EDIT
        if (type.isLeftClick() && !type.isShiftClick()) {
            new EditorMenu(menuUtil, voucher, voucherManager).open();
            return;
        }

        // RIGHT CLICK → GIVE
        if (type.isRightClick() && !type.isShiftClick()) {
            voucherManager.giveVoucher(p, id);
            p.sendMessage(format("&aGiven voucher &e" + id));
            return;
        }

        // SHIFT CLICK → DELETE
        if (type.isShiftClick()) {
            voucherManager.deleteVoucher(id);
            p.sendMessage(format("&cDeleted voucher: &e" + id));
            super.refresh();
        }
    }

    @Override
    public void setMenuItems() {

        // ---------- BORDER ----------
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, border());
            }
        }

        // Close button
        inventory.setItem(49, button(
                XMaterial.BARRIER.parseMaterial(),
                "&cClose",
                List.of(
                        format("&8Close Button"),
                        "",
                        format("&c&nClick to close this menu")
                )
        ));

        // ---------- GET VOUCHERS ----------
        List<Voucher> vouchers = new ArrayList<>(voucherManager.getVoucherMap().values());

        // ---------- APPLY SEARCH ----------
        String search = menuUtil.getSearch();
        if (search != null && !search.isEmpty()) {
            String s = search.toLowerCase();
            vouchers.removeIf(v ->
                    !v.getId().toLowerCase().contains(s) &&
                            !v.getItemDisplayname().toLowerCase().contains(s)
            );
        }

        // ---------- PAGINATION ----------
        int maxPerPage = CONTENT_SLOTS.length; // always 28
        int start = page * maxPerPage;
        int end = Math.min(start + maxPerPage, vouchers.size());

        // ---------- NO VOUCHERS FOUND ----------
        if (vouchers.isEmpty()) {
            inventory.setItem(22, button(
                    XMaterial.ANVIL.parseMaterial(),
                    "&cNo Vouchers Found",
                    List.of(
                            format("&8Error Button"),
                            "",
                            "&7There are no vouchers to display.",
                            "",
                            "&7Create a new one using:",
                            "&e/voucher create <name>"
                    )
            ));
            return;
        }

        // ------------------------
        // PLACE VOUCHER ITEMS
        // ------------------------
        int contentIndex = 0;

        for (int i = start; i < end; i++) {

            if (contentIndex >= CONTENT_SLOTS.length)
                break;

            int slot = CONTENT_SLOTS[contentIndex];
            Voucher v = vouchers.get(i);

            ItemStack item = new ItemStack(
                    XMaterial.matchXMaterial(v.getItemMaterial()).orElse(XMaterial.PAPER).parseMaterial()
            );

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(format("&a" + v.getId()));

            List<String> lore = new ArrayList<>();
            lore.add("&8Voucher Item");
            lore.add("");
            lore.add("&7" + v.getDescription());
            lore.add("");
            lore.add("&7Left-Click: &eEdit");
            lore.add("&7Right-Click: &aGive");
            lore.add("&7Shift-Click: &cDelete");
            meta.setLore(color(lore));
            item.setItemMeta(meta);

            // Add NBT ID
            NBTItem nbti = new NBTItem(item);
            nbti.setString("voucher_id", v.getId());

            inventory.setItem(slot, nbti.getItem());
            contentIndex++;
        }

        // ---------- PAGINATION BUTTONS ----------
        if (page > 0) {
            inventory.setItem(45, button(
                    XMaterial.ARROW.parseMaterial(),
                    "&ePrevious Page",
                    List.of("&7Go back a page")
            ));
        }

        if (vouchers.size() > (page + 1) * maxPerPage) {
            inventory.setItem(53, button(
                    XMaterial.ARROW.parseMaterial(),
                    "&eNext Page",
                    List.of("&7Go forward a page")
            ));
        }

        // ---------- SEARCH BUTTON ----------
        inventory.setItem(47, button(
                XMaterial.OAK_SIGN.parseMaterial(),
                "&bSearch",
                List.of(
                        format("&8Search Button"),
                        "",
                        "&7Search by ID or name.",
                        "&7Click to enter a query.",
                        "",
                        "&b!clear &7= reset search",
                        "&bcancel &7= stop"
                )
        ));

        // ---------- CREATE NEW VOUCHER ----------
        inventory.setItem(51, button(
                XMaterial.EMERALD.parseMaterial(),
                "&aCreate New Voucher",
                List.of(
                        "&8Create Button",
                        "",
                        "&7Create a new voucher.",
                        "",
                        "&a/voucher create <name>"
                )
        ));
    }

    // --------------------------------------
    // Helper item builders
    // --------------------------------------

    private ItemStack border() {
        ItemStack pane = new ItemStack(XMaterial.matchXMaterial("GRAY_STAINED_GLASS_PANE").get().get());
        ItemMeta m = pane.getItemMeta();
        m.setDisplayName(" ");
        pane.setItemMeta(m);
        return pane;
    }

    private ItemStack button(Material mat, String name, List<String> lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(format(name));
        if (lore != null) m.setLore(color(lore));
        i.setItemMeta(m);
        return i;
    }

}
