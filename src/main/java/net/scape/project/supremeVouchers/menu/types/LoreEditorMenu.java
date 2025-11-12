package net.scape.project.supremeVouchers.menu.types;

import com.cryptomorin.xseries.XMaterial;
import net.md_5.bungee.api.ChatColor;
import net.scape.project.supremeVouchers.SupremeVouchers;
import net.scape.project.supremeVouchers.menu.Menu;
import net.scape.project.supremeVouchers.menu.MenuUtil;
import net.scape.project.supremeVouchers.objects.Voucher;
import net.scape.project.supremeVouchers.objects.VoucherInputType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static net.scape.project.supremeVouchers.utils.Utils.format;

public class LoreEditorMenu extends Menu {

    private final Voucher voucher;

    public LoreEditorMenu(MenuUtil menuUtil, Voucher voucher) {
        super(menuUtil);
        this.voucher = voucher;
    }

    @Override
    public String getMenuName() {
        return "Edit Lore: " + voucher.getId();
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null) return;

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        java.util.List<String> lore = voucher.getItemLore();

        // Back
        if (item.getType() == XMaterial.matchXMaterial("ARROW").get().get()) {
            new EditorMenu(menuUtil, voucher, SupremeVouchers.get().getVoucherManager()).open();
            return;
        }

        // Add new
        if (item.getType() == XMaterial.matchXMaterial("GREEN_WOOL").get().get()) {
            lore.add("");
            voucher.setItemLore(lore);
            SupremeVouchers.get().getVoucherManager().save(voucher);
            refresh();
            return;
        }

        // Compute index
        int clickedSlot = e.getSlot();
        int index = indexFromSlot(clickedSlot);
        if (index >= 0 && index < lore.size()) {

            if (e.isShiftClick()) {
                lore.remove(index);
                voucher.setItemLore(lore);
                SupremeVouchers.get().getVoucherManager().save(voucher);
                refresh();
            } else {
                new TextInputMenu(menuUtil, voucher, "Lore " + index, index, VoucherInputType.LORE).open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        fillBorder();

        List<String> lore = voucher.getItemLore();
        int slot = 10;

        for (int i = 0; i < lore.size(); i++) {
            inventory.setItem(slot, loreItem(i + 1, lore.get(i)));

            slot++;
            if (slot % 9 == 8)
                slot += 2; // skip borders
        }

        inventory.setItem(slot, addButton());
        inventory.setItem(49, button(XMaterial.matchXMaterial("ARROW").get().get(), "Back"));
    }

    private ItemStack loreItem(int index, String line) {
        ItemStack i = new ItemStack(XMaterial.matchXMaterial("PAPER").get().get());
        ItemMeta m = i.getItemMeta();

        m.setDisplayName(ChatColor.YELLOW + "Line " + index);

        List<String> lore = new ArrayList<>();
        lore.add(format("&8Voucher Lore Line"));
        lore.add("");
        lore.add(format("&7Value: &f" + line));
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
        m.setDisplayName(format("&aAdd New Line"));
        m.setLore(List.of(
                format("&7Click to add a new Lore Line.")
        ));
        i.setItemMeta(m);
        return i;
    }

    private void fillBorder() {
        ItemStack pane = new ItemStack(XMaterial.matchXMaterial("GRAY_STAINED_GLASS_PANE").get().get());
        ItemMeta m = pane.getItemMeta();
        m.setDisplayName(" ");
        pane.setItemMeta(m);

        for (int i = 0; i < getSlots(); i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                getInventory().setItem(i, pane);
            }
        }
    }

    private int indexFromSlot(int slot) {
        int index = 0;
        for (int s = 10; s < 45; s++) {
            if (s % 9 == 0 || s % 9 == 8) continue;
            if (s == slot) return index;
            index++;
        }
        return -1;
    }

    private ItemStack button(Material mat, String name) {
        ItemStack i = new ItemStack(XMaterial.matchXMaterial(mat).get());
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.YELLOW + name);
        i.setItemMeta(m);
        return i;
    }
}
