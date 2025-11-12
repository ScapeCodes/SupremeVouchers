package net.scape.project.supremeVouchers.menu.types;

import com.cryptomorin.xseries.XMaterial;
import net.md_5.bungee.api.ChatColor;
import net.scape.project.supremeVouchers.SupremeVouchers;
import net.scape.project.supremeVouchers.menu.Menu;
import net.scape.project.supremeVouchers.menu.MenuUtil;
import net.scape.project.supremeVouchers.objects.Voucher;
import net.scape.project.supremeVouchers.objects.VoucherInputType;
import net.scape.project.supremeVouchers.objects.VoucherOptions;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static net.scape.project.supremeVouchers.utils.Utils.format;

public class WorldsEditMenu extends Menu {

    private final Voucher voucher;
    private final VoucherOptions options;

    public WorldsEditMenu(MenuUtil menuUtil, Voucher voucher) {
        super(menuUtil);
        this.voucher = voucher;
        this.options = voucher.getOptions();
    }

    @Override
    public String getMenuName() {
        return "Allowed Worlds: " + voucher.getId();
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

        List<String> worlds = options.getAllowed_worlds();
        String stripped = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        // ✅ Back
        if (item.getType() == XMaterial.matchXMaterial("ARROW").get().get()) {
            new PropertiesMenu(menuUtil, voucher).open();
            return;
        }

        // ✅ Add new world
        if (item.getType() == XMaterial.matchXMaterial("GREEN_WOOL").get().get()) {
            worlds.add("example_new_world");
            options.setAllowed_worlds(worlds);
            SupremeVouchers.get().getVoucherManager().save(voucher);
            refresh();
            return;
        }

        int clickedSlot = e.getSlot();
        int index = indexFromSlot(clickedSlot);
        if (index >= 0 && index < worlds.size()) {

            if (e.isShiftClick()) {
                worlds.remove(index);
                options.setAllowed_worlds(worlds);
                SupremeVouchers.get().getVoucherManager().save(voucher);
                refresh();
            } else {
                new TextInputMenu(
                        menuUtil,
                        voucher,
                        "World " + index,
                        index,
                        VoucherInputType.WORLDS
                ).open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        fillBorder();

        List<String> worlds = options.getAllowed_worlds();
        int slot = 10;

        for (int i = 0; i < worlds.size(); i++) {
            if (slot >= 45) break;

            inventory.setItem(slot, worldItem(i + 1, worlds.get(i)));

            slot++;
            if (slot % 9 == 8) slot += 2; // Skip border
        }

        inventory.setItem(slot, addButton());
        inventory.setItem(49, button(XMaterial.matchXMaterial("ARROW").get().get(), "&eBack"));
    }


    // ------------------------------------------------------
    // Item Builders
    // ------------------------------------------------------

    private ItemStack worldItem(int index, String world) {
        ItemStack i = new ItemStack(XMaterial.matchXMaterial("MAP").get().get());
        ItemMeta m = i.getItemMeta();

        m.setDisplayName(ChatColor.YELLOW + "World " + index);

        List<String> lore = new ArrayList<>();
        lore.add(format("&8Allowed World"));
        lore.add("");
        lore.add(format("&7Value: &f" + world));
        lore.add("");
        lore.add(format("&7Click to &eedit"));
        lore.add(format("&7Shift-Click to &cremove"));

        m.setLore(lore);
        i.setItemMeta(m);
        return i;
    }

    private ItemStack addButton() {
        ItemStack i = new ItemStack(XMaterial.matchXMaterial("GREEN_WOOL").get().get());
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(format("&aAdd New World"));
        m.setLore(List.of(format("&7Click to add a new world.")));
        i.setItemMeta(m);
        return i;
    }

    private void fillBorder() {
        ItemStack pane = new ItemStack(XMaterial.matchXMaterial("GRAY_STAINED_GLASS_PANE").get().get());
        ItemMeta m = pane.getItemMeta();
        m.setDisplayName(" ");
        pane.setItemMeta(m);

        for (int i = 0; i < getSlots(); i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8)
                inventory.setItem(i, pane);
        }
    }

    // ✅ Same index logic as ActionsEditorMenu
    private int indexFromSlot(int slot) {
        int index = 0;
        for (int s = 10; s < 45; s++) {
            if (s % 9 == 0 || s % 9 == 8) continue; // Borders
            if (s == slot) return index;
            index++;
        }
        return -1;
    }

    private ItemStack button(Material mat, String name) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(format(name));
        i.setItemMeta(m);
        return i;
    }
}
