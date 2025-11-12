package net.scape.project.supremeVouchers.menu.types;

import com.cryptomorin.xseries.XMaterial;
import net.scape.project.supremeVouchers.menu.Menu;
import net.scape.project.supremeVouchers.menu.MenuUtil;
import net.scape.project.supremeVouchers.objects.Voucher;
import net.scape.project.supremeVouchers.managers.VoucherManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

import static net.scape.project.supremeVouchers.utils.Utils.format;

public class EditorMenu extends Menu {

    private final Voucher voucher;
    private final VoucherManager voucherManager;

    public EditorMenu(MenuUtil menuUtil, Voucher voucher, VoucherManager voucherManager) {
        super(menuUtil);
        this.voucher = voucher;
        this.voucherManager = voucherManager;
    }

    @Override
    public String getMenuName() {
        return "Edit: " + voucher.getId();
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
        if (name == null) return;

        if (name.equalsIgnoreCase(" ") || name.isBlank()) return;

        switch (name.toLowerCase()) {
            case "remove after use" -> {
                voucher.setRemoveAfterUse(!voucher.removeItemAfterUse());
                voucherManager.save(voucher);
                refresh();
            }
            case "glow" -> {
                voucher.setGlow(!voucher.isGlow());
                voucherManager.save(voucher);
                refresh();
            }
            case "hide toolbar" -> {
                voucher.setHideToolbar(!voucher.isHideToolbar());
                voucherManager.save(voucher);
                refresh();
            }

            case "lore" -> new LoreEditorMenu(menuUtil, voucher).open();

            case "actions" -> new ActionsEditorMenu(menuUtil, voucher).open();

            case "properties" -> new PropertiesMenu(menuUtil, voucher).open();

            case "close" -> p.closeInventory();

            default -> new TextInputMenu(menuUtil, voucher, name).open();
        }
    }

    @Override
    public void setMenuItems() {

        // Border
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, border());
            }
        }

        // Toggles
        inventory.setItem(11, toggle("Remove After Use", voucher.removeItemAfterUse()));
        inventory.setItem(13, toggle("Glow", voucher.isGlow()));
        inventory.setItem(15, toggle("Hide Toolbar", voucher.isHideToolbar()));

        // Editable fields
        inventory.setItem(20, field(XMaterial.matchXMaterial("NAME_TAG").get().get(), "Display Name", voucher.getItemDisplayname()));
        inventory.setItem(21, field(XMaterial.matchXMaterial("COMPASS").get().get(), "Material", voucher.getItemMaterial()));
        inventory.setItem(22, field(XMaterial.matchXMaterial("PAPER").get().get(), "Amount", voucher.getItemAmount()));
        inventory.setItem(23, field(XMaterial.matchXMaterial("GOLD_INGOT").get().get(), "Chance", voucher.getChance()));
        inventory.setItem(24, field(XMaterial.matchXMaterial("BARRIER").get().get(), "Custom Model Data", voucher.getCustomModelData()));

        // List editors
        inventory.setItem(30, field(XMaterial.matchXMaterial("BOOK").get().get(), "Lore", voucher.getItemLore()));
        inventory.setItem(31, field(XMaterial.matchXMaterial("COMPARATOR").get().get(), "Properties", "Edit Voucher Properties"));
        inventory.setItem(32, field(XMaterial.matchXMaterial("WRITABLE_BOOK").get().get(), "Actions", voucher.getActions()));

        // close
        inventory.setItem(49, button(XMaterial.matchXMaterial("BARRIER").get().get(), "&cClose"));
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

    private ItemStack toggle(String name, boolean enabled) {
        ItemStack item = new ItemStack(enabled ? XMaterial.matchXMaterial("GREEN_WOOL").get().get() : XMaterial.matchXMaterial("RED_WOOL").get().get());
        ItemMeta m = item.getItemMeta();
        m.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + name);
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
        m.setDisplayName(ChatColor.YELLOW + name);

        ArrayList<String> lore = new ArrayList<>();
        lore.add(format("&8Voucher"));
        lore.add("");
        lore.add(format("&eCurrent:"));

        if (value instanceof java.util.List<?> list) {
            for (Object o : list) lore.add(format("&7-&b " + o));
        } else {
            lore.add(format("&7-&b " + value));
        }

        lore.add("");
        lore.add(format("&7Click to edit."));

        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    private ItemStack button(Material mat, String name) {
        ItemStack i = new ItemStack(XMaterial.matchXMaterial(mat).get());
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(format(name));
        i.setItemMeta(m);
        return i;
    }
}