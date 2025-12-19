package net.scape.project.supremeVouchers.listeners;

import de.tr7zw.nbtapi.NBTItem;
import me.chancesd.pvpmanager.PvPManager;
import me.chancesd.pvpmanager.player.CombatPlayer;
import net.scape.project.supremeVouchers.SupremeVouchers;
import net.scape.project.supremeVouchers.managers.VoucherManager;
import net.scape.project.supremeVouchers.objects.Voucher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static net.scape.project.supremeVouchers.utils.Utils.*;

public class VoucherListener implements Listener {

    private final VoucherManager voucherManager;
    private FileConfiguration config = SupremeVouchers.get().getConfig();
    private final Map<UUID, Long> lastUse = new HashMap<>(); // cooldown map

    private List<UUID> confirm_use_list = new ArrayList<>();

    public VoucherListener(VoucherManager voucherManager) {
        this.voucherManager = voucherManager;
    }

    @EventHandler
    public void onVoucherUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getHand() == EquipmentSlot.OFF_HAND && config.getBoolean("settings.block-in-offhand", true)) {
            return;
        }

        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) return;

        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasNBTData() || !nbt.hasTag("voucherId")) return;

        String voucherId = nbt.getString("voucherId");
        Voucher voucher = voucherManager.get(voucherId);
        if (voucher == null) return;

        if (voucher.getOptions().isAllowed_worlds_enable()) {
            boolean isNotInAllowedWorld = !voucher.getOptions().getAllowed_worlds().contains(player.getWorld().getName());

            if (isNotInAllowedWorld) {
                msgPlayer(player, voucher.getOptions().getAllowed_worlds_message());
                return;
            }
        }

        if (voucher.getOptions().isCombatActivation()) {
            PvPManager pvpmanager = null;

            if (Bukkit.getPluginManager().isPluginEnabled("PvPManager"))
                pvpmanager = (PvPManager) Bukkit.getPluginManager().getPlugin("PvPManager");
            if (pvpmanager == null)
                return;

            CombatPlayer combatPlayer = pvpmanager.getPlayerManager().get(player);

            if (combatPlayer.isInCombat()) {
                msgPlayer(player, voucher.getOptions().getCombatActivationMessage());
                return;
            }
        }

        if (voucher.getOptions().isConfirm_use_enable()) {
            if (!confirm_use_list.contains(player.getUniqueId())) {
                confirm_use_list.add(player.getUniqueId());
                msgPlayer(player, voucher.getOptions().getConfirm_use_message());
                return;
            } else {
                confirm_use_list.remove(player.getUniqueId());
            }
        }

        List<String> commands = nbt.getObject("voucherActions", List.class);
        if (commands == null || commands.isEmpty()) return;

        // Blocked worlds
        List<String> blockedWorlds = config.getStringList("settings.restrict-worlds");
        if (blockedWorlds.contains(player.getWorld().getName())) {
            player.sendMessage(msgConfig("no-world"));
            return;
        }

        // Permission
        if (config.getBoolean("settings.permission-required", false)) {
            String perm = config.getString("settings.permission", "voucher.redeem");
            if (!player.hasPermission(perm)) {
                player.sendMessage(msgConfig("error.no-permission"));
                return;
            }
        }

        // Cooldown
        int delay = config.getInt("settings.delay", 0);
        if (delay > 0) {
            long now = System.currentTimeMillis();
            long last = lastUse.getOrDefault(player.getUniqueId(), 0L);
            if (now - last < delay * 1000L) {
                player.sendMessage(msgConfig("cooldown"));
                return;
            }
            lastUse.put(player.getUniqueId(), now);
        }

        event.setCancelled(true);

        if (voucher.removeItemAfterUse()) {
            int amt = item.getAmount();
            if (amt > 1) item.setAmount(amt - 1);
            else player.getInventory().remove(item);
        }

        // Execute voucher actions
        for (String a : commands) {
            String action = a.replace("{player}", player.getName())
                    .replace("%player%", player.getName());

            if (a.startsWith("[CONSOLE]")) {
                action = action.replace("[CONSOLE] ", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), format(action));
            }

            else if (a.startsWith("[PLAYER]")) {
                action = action.replace("[PLAYER] ", "");
                player.performCommand(format(action));
            }

            else if (a.startsWith("[MESSAGE]")) {
                action = action.replace("[MESSAGE] ", "");
                player.sendMessage(format(action));
            }

            else if (a.startsWith("[BROADCAST]")) {
                action = action.replace("[BROADCAST] ", "");
                Bukkit.broadcastMessage(format(action));
            }

            else if (a.startsWith("[KIT]")) {
                action = action.replace("[KIT] ", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kit " + action + " " + player.getName());
            }

            else if (a.startsWith("[TELEPORT]")) {
                action = action.replace("[TELEPORT] ", "").trim();
                String[] parts = action.split("\\s+");

                if (parts.length < 3) {
                    player.sendMessage(msgConfig("teleport-invalid-format", Map.of(
                            "input", action
                    )));
                    continue;
                }

                try {
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    double z = Double.parseDouble(parts[2]);

                    float yaw = (parts.length >= 4) ? Float.parseFloat(parts[3]) : player.getLocation().getYaw();
                    float pitch = (parts.length >= 5) ? Float.parseFloat(parts[4]) : player.getLocation().getPitch();

                    String worldName = (parts.length >= 6) ? parts[5] : player.getWorld().getName();

                    var world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        player.sendMessage(msgConfig("teleport-world-not-found", Map.of(
                                "world", worldName
                        )));
                        continue;
                    }

                    var target = new Location(world, x, y, z, yaw, pitch);
                    player.teleport(target);

//                    player.sendMessage(msgConfig("teleport-success", Map.of(
//                            "x", parts[0],
//                            "y", parts[1],
//                            "z", parts[2],
//                            "world", worldName
//                    )));
                } catch (NumberFormatException ex) {
                    player.sendMessage(msgConfig("teleport-invalid-format", Map.of(
                            "input", action
                    )));
                }
            }
        }
    }

    @EventHandler
    public void onVoucherBlockedUse(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) return;

        NBTItem nbt = new NBTItem(current);
        if (!nbt.hasNBTData() || !nbt.hasTag("voucherId")) return;

        // Allow movement inside player's inventory
        if (event.getClickedInventory() != null &&
                event.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        InventoryType type = event.getInventory().getType();

        // Shift-click logic
        if (event.isShiftClick() && type != InventoryType.PLAYER && isRestricted(type)) {
            event.setCancelled(true);
            player.sendMessage(msgConfig("move-block"));
            return;
        }

        // Restriction: ANVIL
        if (type == InventoryType.ANVIL && config.getBoolean("settings.block-in-anvil", true)) {
            event.setCancelled(true);
            player.sendMessage(msgConfig("anvil-block"));
            return;
        }

        // Restriction: GRINDSTONE
        if (type == InventoryType.GRINDSTONE && config.getBoolean("settings.block-in-grindstone", true)) {
            event.setCancelled(true);
            player.sendMessage(msgConfig("grindstone-block"));
            return;
        }

        // Restriction: ENCHANTER
        if (type == InventoryType.ENCHANTING && config.getBoolean("settings.block-in-enchanter", true)) {
            event.setCancelled(true);
            player.sendMessage(msgConfig("enchanter-block"));
            return;
        }

        // Restriction: WORKBENCH only (not player crafting grid)
        if (type == InventoryType.WORKBENCH && config.getBoolean("settings.block-in-crafting", true)) {
            event.setCancelled(true);
            player.sendMessage(msgConfig("crafting-block"));
        }
    }

    @EventHandler
    public void onVoucherHopperMove(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;

        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasNBTData() || !nbt.hasTag("voucherId")) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onVoucherCreativeMiddleClick(InventoryCreativeEvent event) {
        ItemStack item = event.getCursor();
        if (item == null || item.getType().isAir()) return;

        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasNBTData() || !nbt.hasTag("voucherId")) return;

        event.setCancelled(true);
    }

    private boolean isRestricted(InventoryType type) {
        return (type == InventoryType.ANVIL && config.getBoolean("settings.block-in-anvil", true))
                || (type == InventoryType.GRINDSTONE && config.getBoolean("settings.block-in-grindstone", true))
                || (type == InventoryType.ENCHANTING && config.getBoolean("settings.block-in-enchanter", true))
                || ((type == InventoryType.WORKBENCH)
                && config.getBoolean("settings.block-in-crafting", true));
    }
}