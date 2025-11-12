package net.scape.project.supremeVouchers.menu.types;

import com.cryptomorin.xseries.XMaterial;
import net.scape.project.supremeVouchers.SupremeVouchers;
import net.scape.project.supremeVouchers.managers.VoucherManager;
import net.scape.project.supremeVouchers.menu.Menu;
import net.scape.project.supremeVouchers.menu.MenuUtil;
import net.scape.project.supremeVouchers.objects.Voucher;
import net.scape.project.supremeVouchers.objects.VoucherInputType;
import net.scape.project.supremeVouchers.objects.VoucherOptions;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

import static net.scape.project.supremeVouchers.utils.Utils.format;

public class PropertiesMenu extends Menu {

    private final Voucher voucher;
    private final VoucherOptions options;
    private final VoucherManager voucherManager;

    public PropertiesMenu(MenuUtil menuUtil, Voucher voucher) {
        super(menuUtil);
        this.voucher = voucher;
        this.options = voucher.getOptions();
        this.voucherManager = SupremeVouchers.get().getVoucherManager();
    }

    @Override
    public String getMenuName() {
        return "Properties: " + voucher.getId();
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

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (name == null || name.isBlank()) return;

        switch (name.toLowerCase()) {

            // ==========================================================
            // ✅ Confirm Use Options
            // ==========================================================

            case "confirm use" -> {
                options.setConfirm_use_enable(!options.isConfirm_use_enable());
                voucherManager.save(voucher);
                refresh();
            }

            case "confirm message" -> {
                new TextInputMenu(menuUtil, voucher, "confirm message").open();
            }

            // ==========================================================
            // ✅ World Restrictions
            // ==========================================================

            case "allowed worlds" -> {
                options.setAllowed_worlds_enable(!options.isAllowed_worlds_enable());
                voucherManager.save(voucher);
                refresh();
            }

            case "edit worlds" -> new WorldsEditMenu(menuUtil, voucher).open();

            case "worlds deny message" -> {
                new TextInputMenu(menuUtil, voucher, "worlds deny message").open();
            }

            // ==========================================================
            // ✅ Close / Back
            // ==========================================================

            case "back" -> new EditorMenu(menuUtil, voucher, voucherManager).open();

            case "close" -> p.closeInventory();
        }
    }

    @Override
    public void setMenuItems() {

        // ---------------------------------------------------
        // ✅ Border identical to EditorMenu
        // ---------------------------------------------------
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, border());
            }
        }

        // ---------------------------------------------------
        // ✅ Confirm Use Options (row 2)
        // ---------------------------------------------------
        inventory.setItem(12, toggle("Confirm Use", options.isConfirm_use_enable()));
        inventory.setItem(21, field(XMaterial.matchXMaterial("OAK_SIGN").get().get(), "Confirm Message", options.getConfirm_use_message()));

        // ---------------------------------------------------
        // ✅ World Restrictions (row 3)
        // ---------------------------------------------------
        inventory.setItem(14, toggle("Allowed Worlds", options.isAllowed_worlds_enable()));
        inventory.setItem(23, field(XMaterial.matchXMaterial("GRASS_BLOCK").get().get(), "Edit Worlds", options.getAllowed_worlds()));
        inventory.setItem(32, field(XMaterial.matchXMaterial("OAK_SIGN").get().get(), "Worlds Deny Message", options.getAllowed_worlds_message()));

        // ---------------------------------------------------
        // ✅ Back + Close
        // ---------------------------------------------------
        inventory.setItem(49, button(XMaterial.matchXMaterial("ARROW").get().get(), "&cBack"));
    }

    // ===================================================================
    // ✅ Helper item builder methods (copied from EditorMenu)
    // ===================================================================

    private ItemStack border() {
        ItemStack pane = new ItemStack(XMaterial.matchXMaterial("GRAY_STAINED_GLASS_PANE").get().get());
        ItemMeta m = pane.getItemMeta();
        m.setDisplayName(" ");
        pane.setItemMeta(m);
        return pane;
    }

    private ItemStack toggle(String name, boolean enabled) {
        ItemStack item = new ItemStack(enabled ? XMaterial.matchXMaterial("GREEN_WOOL").get().get() : XMaterial.matchXMaterial("RED_WOOL").get().get());
        ItemMeta m = item.getItemMeta();

        m.setDisplayName(format(
                enabled ? "&a" + name : "&c" + name
        ));

        m.setLore(java.util.Arrays.asList(
                format("&8Voucher"),
                "",
                format("&eCurrent: " + (enabled ? "&aEnabled" : "&cDisabled")),
                "",
                format("&7Click to toggle")
        ));

        item.setItemMeta(m);
        return item;
    }


    private ItemStack field(Material mat, String name, Object value) {
        ItemStack item = new ItemStack(XMaterial.matchXMaterial(mat).get());
        ItemMeta m = item.getItemMeta();

        m.setDisplayName(format("&e" + name));

        ArrayList<String> lore = new ArrayList<>();
        lore.add(format("&8Voucher"));
        lore.add("");
        lore.add(format("&eCurrent:"));

        if (value instanceof java.util.List<?> list) {
            for (Object o : list) {
                lore.add(format("&7- &b" + o));
            }
        } else {
            lore.add(format("&7- &b" + value));
        }

        lore.add("");
        lore.add(format("&7Click to edit."));

        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    private ItemStack button(Material mat, String name) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(format(name));
        i.setItemMeta(m);
        return i;
    }
}