package net.scape.project.supremeVouchers.menu;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

import static net.scape.project.supremeVouchers.utils.Utils.*;

public abstract class Menu implements InventoryHolder {

    protected Inventory inventory;
    protected MenuUtil menuUtil;

    /** Whether this menu should auto-update items. */
    private boolean autoUpdate = false;
    private boolean updateTaskRunning = false;

    /** Task ID for updating items. */
    private int updateTask = -1;

    public Menu(MenuUtil menuUtil) {
        this.menuUtil = menuUtil;
    }

    public abstract String getMenuName();

    public abstract int getSlots();

    public abstract void handleMenu(org.bukkit.event.inventory.InventoryClickEvent e);

    /**
     * Build or rebuild items into the inventory.
     * IMPORTANT: This must NOT create a new inventory.
     */
    public abstract void setMenuItems();

    /**
     * Initial opening of menu. Creates inventory ONCE.
     */
    public void open() {
        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());
        setMenuItems();
        menuUtil.getOwner().openInventory(inventory);

        if (autoUpdate) startAutoUpdate();
    }

    /**
     * Refreshes the items WITHOUT reopening the inventory.
     * This is the key to stop flickering.
     */
    public void refresh() {
        if (inventory == null) return;

        inventory.clear();
        setMenuItems();
        menuUtil.getOwner().updateInventory();
    }

    /**
     * Updates a single slot smoothly without resetting GUI.
     */
    public void updateItem(int slot, ItemStack item) {
        if (inventory != null) {
            inventory.setItem(slot, item);
        }
    }

    /**
     * Enables auto-updating every X ticks.
     */
    public void enableAutoUpdate(boolean enable) {
        this.autoUpdate = enable;
    }


    private void startAutoUpdate() {
        Player player = menuUtil.getOwner();

        stopAutoUpdate();
        updateTaskRunning = true;

        Runnable loop = new Runnable() {
            @Override
            public void run() {

                if (!updateTaskRunning) return;
                if (player == null || !player.isOnline()) {
                    stopAutoUpdate();
                    return;
                }
                if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof Menu)) {
                    stopAutoUpdate();
                    return;
                }

                runAsync(() -> {
                    // async animation / building items
                    runMain(() -> refresh());
                });

                // ✅ schedule next tick safely with delay
                runMainLater(this, 1L);
            }
        };

        runMainLater(loop, 1L);
    }

    private void stopAutoUpdate() {
        updateTaskRunning = false;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // SAME ITEM METHODS YOU ALREADY USE

    public ItemStack makeItem(Material material, String displayName, int custom_model_data, boolean hideTooltip, String... lore) {
        return buildItem(XMaterial.matchXMaterial(material).get(), displayName, custom_model_data, hideTooltip, Arrays.asList(lore));
    }

    public ItemStack makeItem(Material material, String displayName, int custom_model_data, List<String> lore) {
        return buildItem(XMaterial.matchXMaterial(material).get(), displayName, custom_model_data, false, lore);
    }

    public ItemStack makeItem(Material material, String displayName, List<String> lore) {
        return buildItem(XMaterial.matchXMaterial(material).get(), displayName, 0, false, lore);
    }

    private ItemStack buildItem(Material material, String displayName, int customModelData, boolean hideTooltip, List<String> lore) {
        ItemStack item = new ItemStack(XMaterial.matchXMaterial(material).get());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(format(displayName));

            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            if (hideTooltip && isPaperVersionAtLeast(1, 21, 5)) {
                meta.setHideTooltip(true);
            }

            meta.setLore(color(lore));
            item.setItemMeta(meta);
        }

        return item;
    }

    public void fillEmpty() {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, makeItem(XMaterial.matchXMaterial("GRAY_STAINED_GLASS_PANE").get().get(), "&6", 0, true));
            }
        }
    }
}