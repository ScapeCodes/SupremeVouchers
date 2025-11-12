package net.scape.project.supremeVouchers.menu;

import com.cryptomorin.xseries.inventory.XInventoryView;
import net.scape.project.supremeVouchers.SupremeVouchers;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static net.scape.project.supremeVouchers.menu.MenuUtil.removeMenu;

public class MenuListener implements Listener {

    // Safely get top inventory (reflection for older versions)
    public static Inventory getTopInventory(InventoryEvent event) {
        try {
            Object view = event.getView();
            Method getTopInventory = view.getClass().getMethod("getTopInventory");
            getTopInventory.setAccessible(true);
            return (Inventory) getTopInventory.invoke(view);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return XInventoryView.of(event.getView()).getTopInventory();
        }
    }

    // ----------------------------------------------------------------------
    // CLICK HANDLING
    // ----------------------------------------------------------------------
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Inventory topInventory = getTopInventory(e);
        if (topInventory == null) return;

        InventoryHolder topHolder = topInventory.getHolder();

        if (topHolder instanceof Menu menu) {
            e.setCancelled(true);

            // Only allow clicks inside the GUI itself
            if (e.getClickedInventory() != null && e.getClickedInventory().equals(topInventory)) {
                ItemStack clickedItem = e.getCurrentItem();

                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    menu.handleMenu(e);
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // DRAG BLOCKER
    // ----------------------------------------------------------------------
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Inventory topInventory = getTopInventory(e);
        if (topInventory == null) return;

        InventoryHolder topHolder = topInventory.getHolder();
        if (topHolder instanceof Menu) {
            e.setCancelled(true);
        }
    }

    // ----------------------------------------------------------------------
    // CLOSE HANDLING
    // ----------------------------------------------------------------------
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        InventoryHolder holder = e.getInventory().getHolder();

        if (holder instanceof Menu) {
            // Clean up MenuUtil chat callbacks
            removeMenu(player);
        }
    }

    // ----------------------------------------------------------------------
    // CHAT INPUT SUPPORT
    // ----------------------------------------------------------------------
    @EventHandler
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        String message = e.getMessage();

        boolean consumed = MenuUtil.handleChat(player, message);

        if (consumed) {
            e.setCancelled(true); // Prevent chat from appearing
        }
    }
}
