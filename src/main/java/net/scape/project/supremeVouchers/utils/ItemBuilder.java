package net.scape.project.supremeVouchers.utils;

import com.cryptomorin.xseries.XMaterial;
import com.nexomc.nexo.api.NexoItems;
import de.tr7zw.nbtapi.NBTItem;
import dev.lone.itemsadder.api.CustomStack;
import io.th0rgal.oraxen.api.OraxenItems;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.valeriishymchuk.simpleitemgenerator.api.SimpleItemGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;

    public ItemBuilder(Player player, String material) {
        if (material.contains("itemsadder-")) {
            String id = material.replace("itemsadder-", "");
            item = getItemWithIA(id);
        } else if (material.contains("hdb-")) {
            int id = Integer.parseInt(material.replace("hdb-", ""));
            HeadDatabaseAPI api = new HeadDatabaseAPI();
            item = api.getItemHead(String.valueOf(id));
        } else if (material.contains("nexo-")) {
            String id = material.replace("nexo-", "");
            item = getItemWithNexo(id);
        } else if (material.contains("oraxen-")) {
            String id = material.replace("oraxen-", "");
            item = getItemWithOraxen(id);
        } else if (material.contains("sig-")) {
            String id = material.replace("sig-", "");
            Optional<ItemStack> resultOpt = SimpleItemGenerator.get().bakeItem(id, null);

            if (!SimpleItemGenerator.get().hasKey(id)) {
                item = new ItemStack(Material.DIRT, 1);
            } else {
                item = resultOpt.orElseGet(() -> new ItemStack(Material.DIRT, 1));
            }
        } else if (material.toLowerCase().startsWith("head-")) {
            String playerName = material.replace("head-", "");

            if (playerName.equalsIgnoreCase("%player_name%")) {
                playerName = player.getName();
            }

            item = new ItemStack(Material.PLAYER_HEAD, 1);
            ItemMeta headMeta = item.getItemMeta();
            if (headMeta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
                item.setItemMeta(skullMeta);
            }
        } else if (material.contains(":")) {
            String[] parts = material.split(":");
            Material mat = Material.valueOf(parts[0].toUpperCase());
            short data = Short.parseShort(parts[1]);
            item = new ItemStack(mat, 1, data);
        } else {
            this.item = new ItemStack(XMaterial.matchXMaterial(material.toUpperCase()).get().get());
            this.meta = this.item.getItemMeta();
        }
    }

    public ItemBuilder(ItemStack base) {
        this.item = base.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder setDisplayName(String name) {
        meta.setDisplayName(name);
        return this;
    }

    public ItemBuilder setCustomModelData(int customModelData) {
        if (customModelData > 0) meta.setCustomModelData(customModelData);
        return this;
    }

    public ItemBuilder setLore(String... lines) {
        List<String> lore = new ArrayList<>();
        for (String line : lines) lore.add(line);
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder setLore(List<String> lines) {
        meta.setLore(new ArrayList<>(lines));
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add(line);
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchant, int level, boolean unsafe) {
        meta.addEnchant(enchant, level, unsafe);
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder hideTooltip(boolean hide) {
        if (meta == null) return this;

        // Use reflection so the plugin works on both legacy (1.8+) and modern servers.
        // setHideTooltip(boolean) only exists on newer Bukkit versions; calling it
        // directly on older versions (like 1.8) would cause NoSuchMethodError.
        try {
            java.lang.reflect.Method m = meta.getClass().getMethod("setHideTooltip", boolean.class);
            m.invoke(meta, hide);
        } catch (NoSuchMethodException ignored) {
            // API not available on this server version – silently ignore.
        } catch (Exception ex) {
            // Any other reflective issue should not break voucher giving.
            ex.printStackTrace();
        }
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder setNBT(String key, String value) {
        NBTItem nbt = new NBTItem(build()); // build to ensure latest meta
        nbt.setString(key, value);
        this.item = nbt.getItem();
        this.meta = this.item.getItemMeta();
        return this;
    }

    public ItemBuilder setNBT(String key, int value) {
        NBTItem nbt = new NBTItem(build());
        nbt.setInteger(key, value);
        this.item = nbt.getItem();
        this.meta = this.item.getItemMeta();
        return this;
    }

    public ItemBuilder setNBT(String key, boolean value) {
        NBTItem nbt = new NBTItem(build());
        nbt.setBoolean(key, value);
        this.item = nbt.getItem();
        this.meta = this.item.getItemMeta();
        return this;
    }

    public ItemBuilder setNBTStringList(String key, List<String> value) {
        NBTItem nbt = new NBTItem(build());
        nbt.setObject(key, value);
        this.item = nbt.getItem();
        this.meta = this.item.getItemMeta();
        return this;
    }

    public String getNBTString(String key) {
        return new NBTItem(item).getString(key);
    }

    public int getNBTInt(String key) {
        return new NBTItem(item).getInteger(key);
    }

    public boolean getNBTBoolean(String key) {
        return new NBTItem(item).getBoolean(key);
    }

    @SuppressWarnings("unchecked")
    public List<String> getNBTStringList(String key) {
        List<String> obj = new NBTItem(item).getObject(key, List.class);
        return obj != null ? obj : new ArrayList<>();
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getItemWithIA(String id) {
        if (CustomStack.isInRegistry(id)) {
            CustomStack stack = CustomStack.getInstance(id);
            if (stack != null) {
                return stack.getItemStack();
            }
        }

        return null;
    }

    public ItemStack getItemWithNexo(String id) {
        return NexoItems.itemFromId(id).build();
    }

    public static ItemStack getItemWithOraxen(String id) {
        return OraxenItems.getItemById(id).build();
    }
}
