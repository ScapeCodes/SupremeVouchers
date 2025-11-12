package net.scape.project.supremeVouchers.managers;

import net.scape.project.supremeVouchers.SupremeVouchers;
import net.scape.project.supremeVouchers.objects.Voucher;
import net.scape.project.supremeVouchers.objects.VoucherOptions;
import net.scape.project.supremeVouchers.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static net.scape.project.supremeVouchers.utils.Utils.format;

public class VoucherManager {

    private final Map<String, Voucher> voucherMap = new HashMap<>();

    // vouchers.yml file + config reference
    private File configFile;
    private FileConfiguration config;

    public VoucherManager() {
        loadVouchersFile();
        load();
    }

    // ============================================================
    //  FILE HANDLING
    // ============================================================

    public void loadVouchersFile() {
        configFile = new File(SupremeVouchers.get().getDataFolder(), "vouchers.yml");

        if (!configFile.exists()) {
            SupremeVouchers.get().saveResource("vouchers.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveVouchersFile() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[Vouchers] Could not save vouchers.yml!");
            e.printStackTrace();
        }
    }

    public FileConfiguration getVouchersConfig() {
        return config;
    }

    // ============================================================
    //  LOADING / SAVING VOUCHERS
    // ============================================================

    public void load() {
        ConfigurationSection voucherSection = config.getConfigurationSection("vouchers");
        if (voucherSection == null) return;

        voucherMap.clear();
        int success = 0;

        for (String id : voucherSection.getKeys(false)) {
            try {
                ConfigurationSection section = voucherSection.getConfigurationSection(id);
                if (section == null) continue;

                String description = section.getString("description", "No description");
                List<String> actions = section.getStringList("actions");
                boolean removeAfterUse = section.getBoolean("removeAfterUse", true);
                String material = section.getString("itemMaterial", "PAPER");
                String displayname = section.getString("itemDisplayname", "§6Voucher");
                int amount = section.getInt("itemAmount", 1);
                List<String> lore = section.getStringList("itemLore");
                boolean glow = section.getBoolean("glow", false);
                boolean hideToolbar = section.getBoolean("hide_toolbar", true);
                int chance = section.getInt("chance", 40);
                int customModelData = section.getInt("itemCustomModelData", 0);

                // Properties
                ConfigurationSection properties = section.getConfigurationSection("properties");

                boolean confirmUseEnable = false;
                String confirmUseMessage = "";
                boolean allowedWorldsEnable = false;
                List<String> allowedWorlds = Collections.emptyList();
                String allowedWorldsMessage = "";

                if (properties != null) {

                    ConfigurationSection confirm = properties.getConfigurationSection("confirm-use");
                    if (confirm != null) {
                        confirmUseEnable = confirm.getBoolean("enable", false);
                        confirmUseMessage = confirm.getString("message", "<red>Are you sure?");
                    }

                    ConfigurationSection worlds = properties.getConfigurationSection("allowed-worlds");
                    if (worlds != null) {
                        allowedWorldsEnable = worlds.getBoolean("enable", false);
                        allowedWorldsMessage = worlds.getString("message", "<red>You cannot use this voucher here!");
                        allowedWorlds = worlds.getStringList("worlds");
                    }
                }

                VoucherOptions options = new VoucherOptions(
                        confirmUseEnable,
                        confirmUseMessage,
                        allowedWorldsEnable,
                        allowedWorlds,
                        allowedWorldsMessage
                );

                Voucher voucher = new Voucher(
                        id, description, actions, removeAfterUse, chance,
                        material, displayname, amount, lore, glow, hideToolbar, customModelData, options
                );

                voucherMap.put(id, voucher);
                success++;

            } catch (Exception e) {
                Bukkit.getLogger().warning("[Vouchers] Failed to load voucher " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        Bukkit.getLogger().info("[Vouchers] Loaded " + success + " vouchers from vouchers.yml");
    }

    public void reload() {
        loadVouchersFile();
        load();
    }

    public void saveToConfig() {
        config.set("vouchers", null);

        for (Voucher v : voucherMap.values()) {
            ConfigurationSection section = config.createSection("vouchers." + v.getId());
            section.set("description", v.getDescription());
            section.set("actions", v.getActions());
            section.set("removeAfterUse", v.removeItemAfterUse());
            section.set("itemMaterial", v.getItemMaterial());
            section.set("itemDisplayname", v.getItemDisplayname());
            section.set("itemCustomModelData", v.getCustomModelData());
            section.set("itemAmount", v.getItemAmount());
            section.set("itemLore", v.getItemLore());
            section.set("glow", v.isGlow());
            section.set("hide_toolbar", v.isHideToolbar());
            section.set("chance", v.getChance());

            // properties
            ConfigurationSection props = section.createSection("properties");

            ConfigurationSection confirm = props.createSection("confirm-use");
            confirm.set("enable", v.getOptions().isConfirm_use_enable());
            confirm.set("message", v.getOptions().getConfirm_use_message());

            ConfigurationSection worlds = props.createSection("allowed-worlds");
            worlds.set("enable", v.getOptions().isAllowed_worlds_enable());
            worlds.set("message", v.getOptions().getAllowed_worlds_message());
            worlds.set("worlds", v.getOptions().getAllowed_worlds());
        }

        saveVouchersFile();
    }

    // ============================================================
    //  GET / GIVE VOUCHER
    // ============================================================

    public Voucher get(String id) {
        if (id == null) return null;
        for (String key : voucherMap.keySet()) {
            if (key.equalsIgnoreCase(id)) return voucherMap.get(key);
        }
        return null;
    }

    public boolean doesExist(String id) {
        if (id == null) return false;
        return voucherMap.keySet().stream().anyMatch(k -> k.equalsIgnoreCase(id));
    }

    public void giveVoucher(Player player, String id) {
        if (!doesExist(id)) return;

        Voucher voucher = get(id);
        if (voucher == null) return;

        List<String> lore = new ArrayList<>();
        for (String line : voucher.getItemLore()) {
            lore.add(format(line));
        }

        ItemStack voucherItem = new ItemBuilder(player, voucher.getItemMaterial())
                .setDisplayName(format(voucher.getItemDisplayname()))
                .setLore(lore.toArray(new String[0]))
                .setAmount(voucher.getItemAmount())
                .setCustomModelData(voucher.getCustomModelData())
                .addEnchant(Enchantment.UNBREAKING, 1, true)
                .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                .hideTooltip(voucher.isHideToolbar())
                .setNBT("voucherId", voucher.getId())
                .setNBTStringList("voucherActions", voucher.getActions())
                .build();

        player.getInventory().addItem(voucherItem);
    }

    public Voucher createVoucher(CommandSender sender, String id) {
        if (id == null || id.isEmpty()) return null;
        if (doesExist(id)) return get(id);

        // Default options
        VoucherOptions options = new VoucherOptions(
                false,
                "&a&lHey! &7Are you sure you want to use this voucher? Right-Click again...",
                false,
                new ArrayList<>(),
                "&c&lHey! <red>You are not in any of the whitelisted worlds."
        );

        List<String> actions = new ArrayList<>();
        actions.add("[CONSOLE] give %player% diamond 1");
        actions.add("[MESSAGE] &bVoucher redeemed! Received 1 diamond...");

        Voucher voucher = new Voucher(
                id,
                "&7New voucher created by " + sender.getName(),
                actions,   // actions
                true,                // removeAfterUse
                40,                  // chance
                "PAPER",             // itemMaterial
                "New Voucher",       // itemDisplayname
                1,                   // amount
                new ArrayList<>(),   // lore
                false,               // glow
                false,                // hideToolbar
                0,                   // customModelData
                options
        );

        voucherMap.put(id, voucher);
        saveToConfig(); // ✅ automatically writes to vouchers.yml

        sender.sendMessage(msg("success.voucher-created", singlePlaceholder("id", id)));

        return voucher;
    }

    public void save(Voucher voucher) {
        if (voucher == null) return;

        String id = voucher.getId();
        if (id == null || id.isEmpty()) return;

        // Path: vouchers.<id>
        ConfigurationSection section = config.getConfigurationSection("vouchers." + id);

        // Create the section if it doesn't exist
        if (section == null) {
            section = config.createSection("vouchers." + id);
        }

        // ================================
        // BASIC FIELDS
        // ================================
        section.set("description", voucher.getDescription());
        section.set("actions", voucher.getActions());
        section.set("removeAfterUse", voucher.removeItemAfterUse());
        section.set("itemMaterial", voucher.getItemMaterial());
        section.set("itemDisplayname", voucher.getItemDisplayname());
        section.set("itemCustomModelData", voucher.getCustomModelData());
        section.set("itemAmount", voucher.getItemAmount());
        section.set("itemLore", voucher.getItemLore());
        section.set("glow", voucher.isGlow());
        section.set("hide_toolbar", voucher.isHideToolbar());
        section.set("chance", voucher.getChance());

        // ================================
        // PROPERTIES
        // ================================
        VoucherOptions opts = voucher.getOptions();

        ConfigurationSection props = section.getConfigurationSection("properties");
        if (props == null)
            props = section.createSection("properties");

        ConfigurationSection confirm = props.getConfigurationSection("confirm-use");
        if (confirm == null)
            confirm = props.createSection("confirm-use");

        confirm.set("enable", opts.isConfirm_use_enable());
        confirm.set("message", opts.getConfirm_use_message());

        ConfigurationSection worlds = props.getConfigurationSection("allowed-worlds");
        if (worlds == null)
            worlds = props.createSection("allowed-worlds");

        worlds.set("enable", opts.isAllowed_worlds_enable());
        worlds.set("message", opts.getAllowed_worlds_message());
        worlds.set("worlds", opts.getAllowed_worlds());

        // ================================
        // SAVE TO FILE
        // ================================
        saveVouchersFile();

        Bukkit.getLogger().info("[Vouchers] Saved voucher '" + id + "' to vouchers.yml");
    }


    private Map<String, String> singlePlaceholder(String key, String value) {
        Map<String, String> m = new HashMap<>();
        m.put(key, value);
        return m;
    }

    /**
     * Reads a message from "messages.<path>" and colorizes it.
     * Example: msg("list.give.text") -> reads messages.list.give.text
     */
    private String msg(String path) {
        String raw = SupremeVouchers.get().getConfig().getString("messages." + path, "&cMissing message: " + path);
        raw = raw.replace("%prefix%", SupremeVouchers.get().getConfig().getString("messages.prefix"));
        return format(raw);
    }

    /**
     * Reads a message and replaces placeholders in the form %key% -> value.
     * The returned string is colorized.
     */
    private String msg(String path, Map<String, String> placeholders) {
        String raw = SupremeVouchers.get().getConfig().getString("messages." + path, "&cMissing message: " + path);

        raw = raw.replace("%prefix%", SupremeVouchers.get().getConfig().getString("messages.prefix"));
        if (placeholders != null) {
            raw = formatPlaceholders(raw, placeholders);
        }
        return format(raw);
    }

    private List<String> listMsg(String path) {
        List<String> list = SupremeVouchers.get().getConfig().getStringList("messages." + path);
        List<String> colored = new ArrayList<>();
        for (String s : list) {
            s = s.replace("%prefix%", SupremeVouchers.get().getConfig().getString("messages.prefix"));
            colored.add(format(s));
        }
        return colored;
    }

    private String formatPlaceholders(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            result = result.replace("%" + e.getKey() + "%", e.getValue());
        }
        return result;
    }

    public void deleteVoucher(String id) {
        SupremeVouchers.get().getVoucherManager().getVouchersConfig().set("vouchers." + id, null);
        SupremeVouchers.get().getVoucherManager().saveVouchersFile();
        reload();
    }


    public Map<String, Voucher> getVoucherMap() {
        return voucherMap;
    }
}
