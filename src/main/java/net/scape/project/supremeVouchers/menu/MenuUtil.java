package net.scape.project.supremeVouchers.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class MenuUtil {

    private final Player owner;
    private String identifier;

    private String search = "";

    private static final Map<UUID, Consumer<String>> chatInputs = new HashMap<>();
    private static final Map<UUID, Menu> openMenus = new HashMap<>();

    public MenuUtil(Player owner, String identifier) {
        this.owner = owner;
        this.identifier = identifier;
    }

    public Player getOwner() {
        return owner;
    }

    public String getIdentifier() {
        return identifier;
    }

    public static void setMenu(Player p, Menu menu) {
        openMenus.put(p.getUniqueId(), menu);
    }

    public static void removeMenu(Player p) {
        openMenus.remove(p.getUniqueId());
    }

    public static Menu getOpenMenu(Player p) {
        return openMenus.get(p.getUniqueId());
    }

    public void awaitChatInput(Consumer<String> input) {
        chatInputs.put(owner.getUniqueId(), input);
    }

    public static boolean handleChat(Player player, String message) {
        Consumer<String> input = chatInputs.remove(player.getUniqueId());
        if (input != null) {
            input.accept(message);
            return true;
        }
        return false;
    }

    public String getSearch() { return search; }
    public void setSearch(String s) { this.search = s; }
}
