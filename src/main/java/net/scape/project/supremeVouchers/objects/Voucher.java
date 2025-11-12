package net.scape.project.supremeVouchers.objects;

import java.util.List;

public class Voucher {

    private final String id;
    private final String description;
    private List<String> actions;
    private boolean removeAfterUse;
    private int chance;
    private VoucherOptions options;

    // item
    private String itemMaterial;
    private String itemDisplayname;
    private int itemAmount;
    private List<String> itemLore;
    private boolean glow;
    private boolean hide_toolbar;
    private int customModelData;

    public Voucher(String id, String description, List<String> actions, boolean removeAfterUse, int chance, String itemMaterial, String itemDisplayname, int itemAmount, List<String> itemLore, boolean glow, boolean hideToolbar, int customModelData, VoucherOptions options) {
        this.id = id;
        this.description = description;
        this.actions= actions;
        this.removeAfterUse = removeAfterUse;
        this.chance = chance;
        this.options = options;
        this.itemMaterial = itemMaterial;
        this.itemDisplayname = itemDisplayname;
        this.itemAmount = itemAmount;
        this.itemLore = itemLore;
        this.glow = glow;
        this.hide_toolbar = hideToolbar;
        this.customModelData = customModelData;
    }

    public String getId() {
        return id;
    }
    public String getDescription() {
        return description;
    }
    public List<String> getActions() {
        return actions;
    }
    public String getItemDisplayname() {
        return itemDisplayname;
    }
    public int getItemAmount() {
        return itemAmount;
    }
    public List<String> getItemLore() {
        return itemLore;
    }
    public boolean isGlow() {
        return glow;
    }
    public boolean isHideToolbar() {
        return hide_toolbar;
    }
    public String getItemMaterial() {
        return itemMaterial;
    }
    public boolean removeItemAfterUse() {
        return removeAfterUse;
    }

    public void setItemDisplayname(String itemDisplayname) {
        this.itemDisplayname = itemDisplayname;
    }

    public void setItemMaterial(String itemMaterial) {
        this.itemMaterial = itemMaterial;
    }

    public void setItemAmount(int itemAmount) {
        this.itemAmount = itemAmount;
    }

    public void setItemLore(List<String> itemLore) {
        this.itemLore = itemLore;
    }

    public void setGlow(boolean glow) {
        this.glow = glow;
    }

    public void setHideToolbar(boolean hide_toolbar) {
        this.hide_toolbar = hide_toolbar;
    }

    public void setRemoveAfterUse(boolean removeAfterUse) {
        this.removeAfterUse = removeAfterUse;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public VoucherOptions getOptions() {
        return options;
    }

    public void setOptions(VoucherOptions options) {
        this.options = options;
    }
}