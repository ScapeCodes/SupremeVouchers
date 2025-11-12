package net.scape.project.supremeVouchers;

import com.artillexstudios.axapi.AxPlugin;
import com.artillexstudios.axapi.utils.featureflags.FeatureFlags;
import net.scape.project.supremeVouchers.commands.VoucherCommand;
import net.scape.project.supremeVouchers.listeners.VoucherListener;
import net.scape.project.supremeVouchers.managers.MergeManager;
import net.scape.project.supremeVouchers.managers.VoucherManager;
import net.scape.project.supremeVouchers.menu.MenuListener;
import net.scape.project.supremeVouchers.menu.MenuUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class SupremeVouchers extends AxPlugin {

    private static SupremeVouchers instance;
    private VoucherManager voucherManager;

    public MergeManager mergeManager;

    private static boolean foliaDetected = false;
    private static boolean foliaChecked = false;

    private static final HashMap<Player, MenuUtil> menuUtilMap = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        voucherManager = new VoucherManager();
        mergeManager = new MergeManager();

        getServer().getPluginManager().registerEvents(new VoucherListener(voucherManager), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        getCommand("voucher").setExecutor(new VoucherCommand());
        getCommand("voucher").setTabCompleter(new VoucherCommand());

        updateFlags();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void reload() {
        super.reloadConfig();

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        voucherManager.reload();
    }

    public static SupremeVouchers get() {
        return instance;
    }

    public VoucherManager getVoucherManager() {
        return voucherManager;
    }

    public MergeManager getMergeManager() {
        return mergeManager;
    }

    public static boolean isFoliaFound() {
        if (!foliaChecked) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                foliaDetected = true;
            } catch (ClassNotFoundException ignored) {
                foliaDetected = false;
            }
            foliaChecked = true; // Cache result forever
        }
        return foliaDetected;
    }

    public HashMap<Player, MenuUtil> getMenuUtil() {
        return menuUtilMap;
    }

    public static MenuUtil getMenuUtilIdentifier(Player player, String identifier) {
        MenuUtil menuUtil;

        if (menuUtilMap.containsKey(player)) {
            return menuUtilMap.get(player);
        } else {
            menuUtil = new MenuUtil(player, identifier);
            menuUtilMap.put(player, menuUtil);
        }

        return menuUtil;
    }

    public void updateFlags() {
        FeatureFlags.USE_LEGACY_HEX_FORMATTER.set(true);
    }
}