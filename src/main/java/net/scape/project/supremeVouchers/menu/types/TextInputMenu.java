package net.scape.project.supremeVouchers.menu.types;

import com.cryptomorin.xseries.XMaterial;
import net.md_5.bungee.api.ChatColor;
import net.scape.project.supremeVouchers.SupremeVouchers;
import net.scape.project.supremeVouchers.menu.Menu;
import net.scape.project.supremeVouchers.menu.MenuUtil;
import net.scape.project.supremeVouchers.objects.Voucher;
import net.scape.project.supremeVouchers.objects.VoucherInputType;
import net.scape.project.supremeVouchers.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

import static net.scape.project.supremeVouchers.utils.Utils.format;
import static net.scape.project.supremeVouchers.utils.Utils.msg;

public class TextInputMenu extends Menu {

    private final Voucher voucher;
    private final String field;
    private final Integer listIndex; // null = single-value
    private final VoucherInputType inputType;

    // ✅ Single-value constructor
    public TextInputMenu(MenuUtil menuUtil, Voucher voucher, String field) {
        super(menuUtil);
        this.voucher = voucher;
        this.field = field;
        this.listIndex = null;
        this.inputType = VoucherInputType.SINGLE_VALUE;
    }

    // ✅ List-editing constructor (lore or actions)
    public TextInputMenu(MenuUtil menuUtil, Voucher voucher, String field, int listIndex, VoucherInputType type) {
        super(menuUtil);
        this.voucher = voucher;
        this.field = field;
        this.listIndex = listIndex;
        this.inputType = type;
    }

    @Override
    public String getMenuName() {
        return "Enter Value:";
    }

    @Override
    public int getSlots() {
        return 9;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        p.closeInventory();

        p.sendMessage(msg("input.suggest-new-input").replace("%id%", field));
        p.sendMessage(msg("input.suggest-cancel"));

        menuUtil.awaitChatInput(input -> {

            if (input.equalsIgnoreCase("cancel")) {
                p.sendMessage(msg("input.cancelled"));
                Utils.runMainLater(() ->
                        new EditorMenu(menuUtil, voucher, SupremeVouchers.get().getVoucherManager()).open(), 1);
                return;
            }

            try {
                if (listIndex != null) {
                    switch (inputType) {

                        case LORE -> {
                            List<String> lore = voucher.getItemLore();
                            lore.set(listIndex, input);
                            voucher.setItemLore(lore);
                        }

                        case ACTIONS -> {
                            // ✅ Validate prefix
                            boolean validPrefix = ACTION_PREFIXES.stream()
                                    .anyMatch(input.toUpperCase()::startsWith);

                            if (!validPrefix) {
                                p.sendMessage(msg("input.invalid-action"));
                                ACTION_PREFIXES.forEach(pre -> p.sendMessage(msg("input.invalid-action-prefix").replace("%action-prefix%", pre)));
                                return;
                            }

                            List<String> actions = voucher.getActions();
                            actions.set(listIndex, input);
                            voucher.setActions(actions);
                        }

                        case WORLDS -> {
                            List<String> worlds = voucher.getOptions().getAllowed_worlds();
                            worlds.set(listIndex, input);
                            voucher.getOptions().setAllowed_worlds(worlds);
                        }
                    }
                } else {
                    switch (field) {
                        case "Display Name" -> voucher.setItemDisplayname(input);
                        case "Material" -> voucher.setItemMaterial(input.toUpperCase());
                        case "Amount" -> voucher.setItemAmount(Integer.parseInt(input));
                        case "Chance" -> voucher.setChance(Integer.parseInt(input));
                        case "Custom Model Data" -> voucher.setCustomModelData(Integer.parseInt(input));
                        case "confirm message" -> voucher.getOptions().setConfirm_use_message(input);
                        case "worlds deny message" -> voucher.getOptions().setAllowed_worlds_message(input);
                    }
                }

                SupremeVouchers.get().getVoucherManager().save(voucher);
                p.sendMessage(msg("voucher-updated"));
            } catch (Exception ex) {
                p.sendMessage(msg("input.invalid-input"));
            }

            Utils.runMainLater(() -> {
                switch (inputType) {
                    case LORE -> new LoreEditorMenu(menuUtil, voucher).open();
                    case WORLDS -> new WorldsEditMenu(menuUtil, voucher).open();
                    case ACTIONS -> new ActionsEditorMenu(menuUtil, voucher).open();
                    default -> new EditorMenu(menuUtil, voucher, SupremeVouchers.get().getVoucherManager()).open();
                }
            }, 1);
        });
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(4, makeItem(
                XMaterial.matchXMaterial("OAK_SIGN").get().get(),
                ChatColor.YELLOW + "Click to type",
                null
        ));
    }

    private static final List<String> ACTION_PREFIXES = List.of(
            "[CONSOLE]",
            "[PLAYER]",
            "[MESSAGE]",
            "[BROADCAST]",
            "[KIT]",
            "[TELEPORT]"
    );
}
