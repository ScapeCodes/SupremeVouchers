package net.scape.project.supremeVouchers.managers;

import net.scape.project.supremeVouchers.SupremeVouchers;
import net.scape.project.supremeVouchers.objects.Voucher;
import net.scape.project.supremeVouchers.objects.VoucherOptions;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.scape.project.supremeVouchers.utils.Utils.format;

public class MergeManager {

    private final File crazyVouchersFolder = new File("plugins/CrazyVouchers/vouchers");
    private final VoucherManager voucherManager = SupremeVouchers.get().getVoucherManager();

    public MergeManager() {}

    public boolean isCrazyVouchers() {
        return crazyVouchersFolder.exists() && crazyVouchersFolder.isDirectory();
    }

    public void merge(CommandSender sender) {
        if (sender == null) {
            sender = Bukkit.getConsoleSender();
        }

        if (!isCrazyVouchers()) {
            sender.sendMessage(format("&c[SupremeVouchers] CrazyVouchers folder not found, skipping merge."));
            return;
        }

        File[] files = crazyVouchersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            sender.sendMessage(format("&c[SupremeVouchers] No CrazyVouchers found to merge."));
            return;
        }

        int merged = 0;
        Yaml yaml = new Yaml();

        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file)) {

                Map<String, Object> data = yaml.load(fis);
                if (data == null || !data.containsKey("voucher")) continue;

                Map<String, Object> vMap = (Map<String, Object>) data.get("voucher");
                String id = file.getName().replace(".yml", "");

                if (voucherManager.doesExist(id)) continue; // Skip duplicates

                /* ---------------------------------------------------------
                 *  BASIC FIELDS
                 * --------------------------------------------------------- */
                String rawItem = vMap.getOrDefault("item", "PAPER").toString();
                String material = rawItem;
                int customModelData = 0;

                if (rawItem.contains("#")) {
                    String[] split = rawItem.split("#");
                    material = split[0];

                    try {
                        customModelData = Integer.parseInt(split[1]);
                    } catch (NumberFormatException ignored) {
                        customModelData = 0;
                    }
                }

                String displayName = vMap.getOrDefault("name", id).toString();
                boolean glow = Boolean.parseBoolean(vMap.getOrDefault("glowing", false).toString());
                int chance = 50;
                int amount = 1;

                if (vMap.containsKey("items")) {
                    List<Object> items = (List<Object>) vMap.get("items");
                    if (items != null)
                        amount = items.size();
                }

                /* ---------------------------------------------------------
                 *  LORE
                 * --------------------------------------------------------- */
                List<String> lore = new ArrayList<>();
                if (vMap.containsKey("lore")) {
                    List<Object> rawLore = (List<Object>) vMap.get("lore");
                    if (rawLore != null)
                        rawLore.forEach(o -> lore.add(o.toString()));
                }

                /* ---------------------------------------------------------
                 *  ACTIONS
                 * --------------------------------------------------------- */
                List<String> actions = new ArrayList<>();
                if (vMap.containsKey("commands")) {
                    List<Object> rawCommands = (List<Object>) vMap.get("commands");
                    if (rawCommands != null)
                        rawCommands.forEach(o -> actions.add("[CONSOLE] " + o));
                }

                boolean hide_tooltip = false;

                /* ---------------------------------------------------------
                 *  PROPERTIES → VoucherOptions
                 * --------------------------------------------------------- */
                boolean confirmUseEnable = false;
                String confirmUseMessage = "<yellow>Are you sure you want to use this voucher? Right-click again.";

                boolean allowedWorldsEnable = false;
                List<String> allowedWorlds = new ArrayList<>();
                String allowedWorldsMessage = "<red>You cannot use this voucher in this world.";

                if (vMap.containsKey("options")) {
                    Map<String, Object> opt = (Map<String, Object>) vMap.get("options");

                    // two-step-authentication == confirm-use
                    if (opt.containsKey("two-step-authentication")) {
                        confirmUseEnable = Boolean.parseBoolean(opt.get("two-step-authentication").toString());
                    }

                    if (opt.containsKey("message")) {
                        confirmUseMessage = opt.get("message").toString();
                    }

                    if (opt.containsKey("whitelist-worlds")) {
                        Map<String, Object> worldMap = (Map<String, Object>) opt.get("whitelist-worlds");

                        allowedWorldsEnable = Boolean.parseBoolean(worldMap.getOrDefault("toggle", false).toString());
                        allowedWorldsMessage = worldMap.getOrDefault("message", allowedWorldsMessage).toString();

                        if (worldMap.containsKey("worlds")) {
                            List<Object> rawWorlds = (List<Object>) worldMap.get("worlds");
                            if (rawWorlds != null)
                                rawWorlds.forEach(o -> allowedWorlds.add(o.toString()));
                        }
                    }
                }

                if (vMap.containsKey("components")) {
                    Map<String, Object> opt = (Map<String, Object>) vMap.get("components");

                    if (opt.containsKey("hide-tooltip")) {
                        hide_tooltip = Boolean.parseBoolean(opt.get("hide-tooltip").toString());
                    }
                }

                VoucherOptions options = new VoucherOptions(
                        confirmUseEnable,
                        confirmUseMessage,
                        allowedWorldsEnable,
                        allowedWorlds,
                        allowedWorldsMessage,
                        false,
                        "&c&lHey! &cYou cannot not redeem a voucher in combat mode."
                );

                /* ---------------------------------------------------------
                 *  CREATE SUPREME VOUCHER
                 * --------------------------------------------------------- */
                Voucher v = new Voucher(
                        id,
                        displayName,
                        actions,
                        true,
                        chance,
                        material,
                        displayName,
                        amount,
                        lore,
                        glow,
                        hide_tooltip,
                        customModelData,
                        options
                );

                voucherManager.getVoucherMap().put(id, v);
                merged++;

            } catch (Exception e) {
                sender.sendMessage(format("&c[SupremeVouchers] Failed to import CrazyVoucher: " + file.getName()));
                e.printStackTrace();
            }
        }

        // ✅ Save imported vouchers into vouchers.yml (NOT config.yml)
        voucherManager.saveToConfig();

        sender.sendMessage(format("&a[SupremeVouchers] Merged " + merged + " vouchers from CrazyVouchers."));
    }
}