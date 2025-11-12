package net.scape.project.supremeVouchers.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.scape.project.supremeVouchers.SupremeVouchers;
import net.scape.project.supremeVouchers.managers.VoucherManager;
import net.scape.project.supremeVouchers.menu.types.EditorMenu;
import net.scape.project.supremeVouchers.menu.types.VouchersMenu;
import net.scape.project.supremeVouchers.objects.Voucher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static net.scape.project.supremeVouchers.utils.Utils.*;

public class VoucherCommand implements CommandExecutor, TabCompleter {

    private VoucherManager voucherManager = SupremeVouchers.get().getVoucherManager();
    private final Random random = new Random();

    // ---------------------------
    // Command Handling
    // ---------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // Permission check for players
        if (sender instanceof Player playerCheck) {
            if (!playerCheck.hasPermission("supremevouchers.admin")) {
                msgPlayer(sender, msg("errors.no-permission"));
                return true;
            }
        }

        if (args.length == 0) {
            if (!SupremeVouchers.get().getConfig().getBoolean("settings.voucher-list-menu")) {
                listMsg("help.main").forEach(sender::sendMessage);
            } else {
                if (sender instanceof Player playerCheck) {
                    SupremeVouchers.get().getMenuUtil().remove(playerCheck);
                    new VouchersMenu(SupremeVouchers.getMenuUtilIdentifier(playerCheck, null), voucherManager).open();
                } else {
                    listMsg("help.main").forEach(sender::sendMessage);
                }
            }
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "create":
                if (args.length < 2) {
                    msgPlayer(sender, msg("errors.usage-create"));
                    return true;
                }

                String createId = args[1];

                if (voucherManager.doesExist(createId)) {
                    msgPlayer(sender, msg("errors.voucher-exists", singlePlaceholder("id", createId)));
                    return true;
                }

                voucherManager.createVoucher(sender, createId);
                return true;

            case "give":
                if (args.length < 3) {
                    msgPlayer(sender, msg("errors.usage-give"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    msgPlayer(sender, msg("errors.player-not-found", singlePlaceholder("player", args[1])));
                    return true;
                }

                String id = args[2];
                if (!voucherManager.doesExist(id)) {
                    msgPlayer(sender, msg("errors.voucher-not-found", singlePlaceholder("id", id)));
                    return true;
                }

                voucherManager.giveVoucher(target, id);

                Map<String, String> gavePlaceholders = new HashMap<>();
                gavePlaceholders.put("id", id);
                gavePlaceholders.put("player", target.getName());

                msgPlayer(sender, msg("success.voucher-given", gavePlaceholders));
                return true;

            case "giveall":
                if (args.length < 2) {
                    msgPlayer(sender, msg("errors.usage-giveall"));
                    return true;
                }

                String v_id = args[1];
                if (!voucherManager.doesExist(v_id)) {
                    msgPlayer(sender, msg("errors.voucher-not-found", singlePlaceholder("id", v_id)));
                    return true;
                }

                for (Player all : Bukkit.getOnlinePlayers()) {
                    voucherManager.giveVoucher(all, v_id);
                }

                Map<String, String> gPlaceholders = new HashMap<>();
                gPlaceholders.put("id", v_id);
                gPlaceholders.put("player", "all players");


                msgPlayer(sender, msg("success.voucher-given", gPlaceholders));
                return true;

            case "giverandom":
                if (args.length < 2) {
                     msgPlayer(sender, msg("errors.usage-giverandom"));
                    return true;
                }

                Player randTarget = Bukkit.getPlayer(args[1]);
                if (randTarget == null) {
                     msgPlayer(sender, msg("errors.player-not-found", singlePlaceholder("player", args[1])));
                    return true;
                }

                Voucher randomVoucher = getRandomVoucher();
                if (randomVoucher == null) {
                     msgPlayer(sender, msg("errors.no-vouchers-available"));
                    return true;
                }

                voucherManager.giveVoucher(randTarget, randomVoucher.getId());

                Map<String, String> rndPlaceholders = new HashMap<>();
                rndPlaceholders.put("id", randomVoucher.getId());
                rndPlaceholders.put("player", randTarget.getName());

                msgPlayer(sender, msg("success.random-voucher-given", rndPlaceholders));
                return true;

            case "reload":
                SupremeVouchers.get().reload();
                msgPlayer(sender, msg("success.vouchers-reloaded"));
                return true;

            case "debug":
                sendDebug(sender);
                return true;

            case "merge":
                SupremeVouchers.get().getMergeManager().merge(sender);
                return true;

            case "list":
                int page = 1;
                if (args.length >= 2) {
                    try {
                        page = Math.max(1, Integer.parseInt(args[1]));
                    } catch (NumberFormatException ignored) {}
                }

                List<Voucher> vouchers = new ArrayList<>(voucherManager.getVoucherMap().values());
                int total = vouchers.size();
                int perPage = 10;
                int maxPage = (int) Math.ceil(total / (double) perPage);

                if (page > maxPage) page = maxPage;
                if (page < 1) page = 1;

                // Send header
                msgPlayer(sender,
                        msg("list.header")
                                .replace("%amount%", String.valueOf(total))
                                .replace("%page%", String.valueOf(page))
                                .replace("%maxpage%", String.valueOf(maxPage))
                );

                int start = (page - 1) * perPage;
                int end = Math.min(start + perPage, total);

                // Console: simple text, no hover/click
                if (!(sender instanceof Player player)) {
                    for (int i = start; i < end; i++) {
                        msgPlayer(sender, "&7" + vouchers.get(i).getId());
                    }
                    if (page < maxPage) {
                        msgPlayer(sender, "&eUse &6/voucher list " + (page + 1) + " &efor next page.");
                    }
                    return true;
                }

                // Player view (Adventure Components)
                for (int i = start; i < end; i++) {
                    Voucher v = vouchers.get(i);

                    String giveText = msg("list.give.text");
                    String giveHover = formatPlaceholders(
                            SupremeVouchers.get().getConfig().getString("messages.list.give.hover"),
                            Map.of("id", v.getId())
                    );

                    String editText = msg("list.edit.text");
                    String editHover = formatPlaceholders(
                            SupremeVouchers.get().getConfig().getString("messages.list.edit.hover"),
                            Map.of("id", v.getId())
                    );

                    String deleteText = msg("list.delete.text");
                    String deleteHover = formatPlaceholders(
                            SupremeVouchers.get().getConfig().getString("messages.list.delete.hover"),
                            Map.of("id", v.getId())
                    );

                    Component line = Component.text(format(msg("list.voucher-name-color") + v.getId() + " &8- "))

                            // Give button
                            .append(Component.text(format(giveText))
                                    .hoverEvent(Component.text(format(giveHover)))
                                    .clickEvent(ClickEvent.runCommand("/voucher give " + player.getName() + " " + v.getId()))
                            )
                            .append(Component.text(" "))

                            // Edit button
                            .append(Component.text(format(editText))
                                    .hoverEvent(Component.text(format(editHover)))
                                    .clickEvent(ClickEvent.runCommand("/voucher edit " + v.getId()))
                            )
                            .append(Component.text(" "))

                            // Delete button
                            .append(Component.text(format(deleteText))
                                    .hoverEvent(Component.text(format(deleteHover)))
                                    .clickEvent(ClickEvent.runCommand("/voucher delete " + v.getId()))
                            );

                    player.sendMessage(line);
                }

                // Pagination
                List<Component> navParts = new ArrayList<>();

                if (page > 1) {
                    navParts.add(
                            Component.text(format(msg("list.back")))
                                    .clickEvent(ClickEvent.runCommand("/voucher list " + (page - 1)))
                    );
                }

                if (page < maxPage) {
                    if (!navParts.isEmpty()) navParts.add(Component.text(" "));
                    navParts.add(
                            Component.text(format(msg("list.next")))
                                    .clickEvent(ClickEvent.runCommand("/voucher list " + (page + 1)))
                    );
                }

                if (!navParts.isEmpty()) {
                    Component nav = Component.empty();
                    for (Component part : navParts) nav = nav.append(part);
                    player.sendMessage(nav);
                }

                return true;


            case "delete":
                if (args.length < 2) {
                    msgPlayer(sender, msg("errors.usage-delete"));
                    return true;
                }

                String deleteId = args[1];
                if (!voucherManager.doesExist(deleteId)) {
                    msgPlayer(sender, msg("errors.voucher-not-found", singlePlaceholder("id", deleteId)));
                    return true;
                }

                SupremeVouchers.get().getVoucherManager().getVouchersConfig().set("vouchers." + deleteId, null);
                SupremeVouchers.get().getVoucherManager().saveVouchersFile();
                voucherManager.reload();

                msgPlayer(sender, msg("success.voucher-deleted", singlePlaceholder("id", deleteId)));
                return true;

            case "edit":
                if (!(sender instanceof Player playerEdit)) {
                     msgPlayer(sender, msg("errors.must-be-player"));
                    return true;
                }

                if (args.length < 2) {
                    playerEdit.sendMessage(msg("errors.usage-edit"));
                    return true;
                }

                String editId = args[1];
                if (!voucherManager.doesExist(editId)) {
                    playerEdit.sendMessage(msg("errors.voucher-not-found", singlePlaceholder("id", editId)));
                    return true;
                }

                // old version
                //editorGUI.openVoucherEditMenu(playerEdit, voucherManager.get(editId));

                // new version
                SupremeVouchers.get().getMenuUtil().remove(sender);
                new EditorMenu(SupremeVouchers.getMenuUtilIdentifier(playerEdit, editId), SupremeVouchers.get().getVoucherManager().get(editId), SupremeVouchers.get().getVoucherManager()).open();
                return true;

            default:
                 msgPlayer(sender, msg("errors.unknown-command"));
                return true;
        }
    }

    // ---------------------------
    // Helper methods
    // ---------------------------

    private Map<String, String> singlePlaceholder(String key, String value) {
        Map<String, String> m = new HashMap<>();
        m.put(key, value);
        return m;
    }

    /**
     * Weighted random voucher selection.
     */
    private Voucher getRandomVoucher() {
        List<Voucher> list = new ArrayList<>(voucherManager.getVoucherMap().values());
        if (list.isEmpty()) return null;

        int total = 0;
        for (Voucher v : list) total += v.getChance();
        if (total <= 0) return null;

        int roll = random.nextInt(total);
        int current = 0;
        for (Voucher v : list) {
            current += v.getChance();
            if (roll < current) return v;
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        // Main subcommand completion
        if (args.length == 1) {
            List<String> subs = List.of("create", "give", "giverandom", "reload", "merge", "list", "delete", "edit", "debug");
            for (String s : subs) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(s);
                }
            }
            return completions;
        }

        // Second argument suggestions
        if (args.length == 2) {

            switch (args[0].toLowerCase()) {

                case "create":
                    // Any text is allowed, but we still return empty (user types ID)
                    return completions;

                case "give":
                case "giverandom":
                    // Suggest online players
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(p.getName());
                        }
                    }
                    return completions;

                case "giveall":
                case "delete":
                case "edit":
                    // Suggest voucher IDs
                    for (String id : voucherManager.getVoucherMap().keySet()) {
                        if (id.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(id);
                        }
                    }
                    return completions;

                case "list":
                    // Suggest page numbers
                    try {
                        int total = voucherManager.getVoucherMap().size();
                        int maxPage = (int) Math.ceil(total / 10.0);
                        for (int i = 1; i <= maxPage; i++) {
                            String pageStr = String.valueOf(i);
                            if (pageStr.startsWith(args[1])) completions.add(pageStr);
                        }
                    } catch (Exception ignored) {}
                    return completions;

            }
        }

        // Third argument suggestions
        if (args.length == 3) {

            if (args[0].equalsIgnoreCase("give")) {
                // Suggest voucher IDs
                for (String id : voucherManager.getVoucherMap().keySet()) {
                    if (id.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(id);
                    }
                }
            }

            return completions;
        }

        return completions;
    }

    public void sendDebug(CommandSender player) {
        msgPlayer(player, "");
        msgPlayer(player, format("&fDebugging SupremeVouchers &8➜"));
        msgPlayer(player, "");

        msgPlayer(player, format("&7Version: &f" + SupremeVouchers.get().getDescription().getVersion()));
        msgPlayer(player, format("&7Author: &fDevScape (aka. Scape)"));
        msgPlayer(player, format("&7Discord: &fhttps://discord.gg/AnPwty8asP"));
        msgPlayer(player, "");

        msgPlayer(player, format("&7Vouchers loaded: &f" + SupremeVouchers.get().getVoucherManager().getVoucherMap().size()));
        msgPlayer(player, format("&7Database Assigned: &fFILE"));
        msgPlayer(player, "");

        msgPlayer(player, format("&e&lPlugins Hooked:"));

        // Vault
        if (pluginExists("Vault") || pluginExists("VaultUnlocked")) {
            msgPlayer(player, format(" &8● &7Vault: &fFound"));
        } else {
            msgPlayer(player, format(" &8● &7Vault: &fNot found"));
        }

        // NBTAPI
        if (pluginExists("NBTAPI")) {
            msgPlayer(player, format(" &8● &7NBTAPI: &fFound"));
        } else {
            msgPlayer(player, format(" &8● &7NBTAPI: &fNot found"));
        }

        // PlaceholderAPI
        if (pluginExists("PlaceholderAPI")) {
            msgPlayer(player, format(" &8● &7PlaceholderAPI: &fFound"));
        } else {
            msgPlayer(player, format(" &8● &7PlaceholderAPI: &fNot found"));
        }
    }

    private boolean pluginExists(String name) {
        return SupremeVouchers.get().getServer().getPluginManager().getPlugin(name) != null;
    }
}