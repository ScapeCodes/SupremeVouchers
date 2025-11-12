package net.scape.project.supremeVouchers.menu.types;

import com.cryptomorin.xseries.XMaterial;
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

public class ActionsEditorMenu extends Menu {

    private final Voucher voucher;

    public ActionsEditorMenu(MenuUtil menuUtil, Voucher voucher) {
        super(menuUtil);
        this.voucher = voucher;
    }

    @Override
    public String getMenuName() {
        return format("Edit Actions: " + voucher.getId());
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getItemMeta() == null) return;

        String name = item.getItemMeta().getDisplayName();
        List<String> actions = voucher.getActions();

        // Back button
        if (item.getType() == XMaterial.matchXMaterial("ARROW").get().get()) {
            new EditorMenu(menuUtil, voucher, SupremeVouchers.get().getVoucherManager()).open();
            return;
        }

        // Add new action
        if (item.getType() == XMaterial.matchXMaterial("GREEN_WOOL").get().get()) {
            actions.add("");
            voucher.setActions(actions);
            SupremeVouchers.get().getVoucherManager().save(voucher);
            refresh();
            return;
        }

        // Action index from clicked slot
        int clickedSlot = e.getSlot();
        int index = indexFromSlot(clickedSlot);
        if (index >= 0 && index < actions.size()) {

            if (e.isShiftClick()) {
                actions.remove(index);
                voucher.setActions(actions);
                SupremeVouchers.get().getVoucherManager().save(voucher);
                refresh();
            } else {
                new TextInputMenu(menuUtil, voucher, "Action " + index, index, VoucherInputType.ACTIONS).open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        fillBorder();

        List<String> actions = voucher.getActions();
        int slot = 10;

        for (int i = 0; i < actions.size(); i++) {
            String action = actions.get(i);
            inventory.setItem(slot, actionItem(i + 1, action));

            slot++;
            if (slot % 9 == 8) slot += 2; // jump over border columns
        }

        inventory.setItem(slot, addButton());
        inventory.setItem(49, button(XMaterial.matchXMaterial("arrow").get().get(), "&7Back"));
    }

    private ItemStack actionItem(int index, String action) {
        ItemStack i = new ItemStack(XMaterial.matchXMaterial("PAPER").get().get());
        ItemMeta m = i.getItemMeta();

        m.setDisplayName(format("&eAction " + index));

        List<String> lore = new ArrayList<>();
        lore.add(format("&7Voucher Action"));
        lore.add("");
        lore.add(format("&7Value: &f" + action));
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
        m.setDisplayName(format("&aAdd New Action"));
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
        m.setDisplayName(format(name));
        i.setItemMeta(m);
        return i;
    }
}