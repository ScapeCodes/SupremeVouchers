package net.scape.project.supremeVouchers.utils;

import com.artillexstudios.axapi.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.scape.project.supremeVouchers.SupremeVouchers;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static Pattern p1 = Pattern.compile("\\{#([0-9A-Fa-f]{6})\\}");
    private static Pattern p2 = Pattern.compile("&#([A-Fa-f0-9]){6}");
    private static Pattern p3 = Pattern.compile("#([A-Fa-f0-9]){6}");
    private static Pattern p4 = Pattern.compile("<#([A-Fa-f0-9])>{6}");
    private static Pattern p5 = Pattern.compile("<#&([A-Fa-f0-9])>{6}");

    private static final Pattern GRADIENT_PATTERN =
            Pattern.compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>");


    private static final Pattern FORMAT_TAG_PATTERN =
            Pattern.compile("<(bold|italic|underlined|strikethrough|obfuscated)>");
    private static final Pattern FORMAT_CLOSE_PATTERN =
            Pattern.compile("</(bold|italic|underlined|strikethrough|obfuscated)>");

    public static String format(String message) {
        if (message == null || message.isEmpty()) return "";

        if (isVersionLessThan("1.16")) {
            message = ChatColor.translateAlternateColorCodes('&', message);
            return message;
        }

        return StringUtils.formatToString(message, Collections.emptyMap());
    }

    /**
     * Safely parses a string into a Component with MiniMessage.
     * Automatically converts legacy codes.
     */
    public static Component mm(String message) {
        if (message == null || message.isEmpty()) return Component.empty();

        try {
            // Legacy detection
            if (message.contains("&") || message.contains("§")) {
                return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            }

            return MiniMessage.miniMessage().deserialize(message);
        } catch (Exception e) {
            return Component.text(ChatColor.stripColor(message));
        }
    }

    /**
     * MiniMessage with placeholders.
     */
    public static Component mm(String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return Component.empty();

        try {
            TagResolver.Builder builder = TagResolver.builder();
            if (placeholders != null) {
                placeholders.forEach((k, v) -> builder.resolver(Placeholder.parsed(k, v)));
            }

            // Legacy detection
            if (message.contains("&") || message.contains("§")) {
                String replaced = formatPlaceholders(message, placeholders);
                return LegacyComponentSerializer.legacyAmpersand().deserialize(replaced);
            }

            return MiniMessage.miniMessage().deserialize(message, builder.build());
        } catch (Exception e) {
            // fallback
            String replaced = formatPlaceholders(message, placeholders);
            return Component.text(ChatColor.stripColor(replaced));
        }
    }

    public static String msgConfig(String path) {
        String text = SupremeVouchers.get().getConfig().getString("messages." + path, "&cMissing message: " + path);
        text = text.replace("%prefix%", SupremeVouchers.get().getConfig().getString("messages.prefix"));

        return format(text);
    }

    public static String msgConfig(String path, Map<String, String> placeholders) {
        String text = SupremeVouchers.get().getConfig().getString("messages." + path, "&cMissing message: " + path);

        text = text.replace("%prefix%", SupremeVouchers.get().getConfig().getString("messages.prefix"));

        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                text = text.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return format(text);
    }

    public static void msgPlayer(CommandSender sender, String... messages) {
        if (messages == null || messages.length == 0)
            return;
        for (String msg : messages) {
            if (msg != null && !msg.isEmpty())
                sender.sendMessage(format(msg));
        }
    }

    public static java.util.List<String> color(List<String> lore) {
        if (lore == null) return Collections.emptyList();
        return lore.stream()
                .map(line -> format(line))
                .collect(Collectors.toList());
    }

    public static boolean isPaperVersionAtLeast(int major, int minor, int patch) {
        String version = Bukkit.getVersion(); // Example: git-Paper-441 (MC: 1.21.5)
        Pattern pattern = Pattern.compile("\\(MC: (\\d+)\\.(\\d+)(?:\\.(\\d+))?\\)");
        Matcher matcher = pattern.matcher(version);

        if (matcher.find()) {
            int majorVer = Integer.parseInt(matcher.group(1));
            int minorVer = Integer.parseInt(matcher.group(2));
            int patchVer = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;

            if (majorVer > major) return true;
            if (majorVer == major && minorVer > minor) return true;
            if (majorVer == major && minorVer == minor && patchVer >= patch) return true;
        }

        return false;
    }

    /**
     * Backward-compatible overload for Player, calls main method.
     */
    public static void msgPlayer(Player player, String... messages) {
        msgPlayer((CommandSender) player, messages);
    }

    private static String applyGradient(String text, String startHex, String endHex, String formats) {
        Color start = Color.decode(startHex);
        Color end = Color.decode(endHex);

        int len = text.length();
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < len; i++) {
            float ratio = (float) i / (float) Math.max(1, len - 1);

            int r = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int g = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int b = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

            String hex = String.format("#%02x%02x%02x", r, g, b);

            out.append(ChatColor.of(hex)).append(formats).append(text.charAt(i));
        }

        return out.toString();
    }

    private static String extractFormatting(String text) {
        StringBuilder sb = new StringBuilder();

        Matcher matcher = FORMAT_TAG_PATTERN.matcher(text);
        while (matcher.find()) {
            String tag = matcher.group(1);

            switch (tag) {
                case "bold": sb.append(ChatColor.BOLD); break;
                case "italic": sb.append(ChatColor.ITALIC); break;
                case "underlined": sb.append(ChatColor.UNDERLINE); break;
                case "strikethrough": sb.append(ChatColor.STRIKETHROUGH); break;
                case "obfuscated": sb.append(ChatColor.MAGIC); break;
            }
        }
        return sb.toString();
    }

    private static String removeFormattingTags(String text) {
        text = FORMAT_TAG_PATTERN.matcher(text).replaceAll("");
        text = FORMAT_CLOSE_PATTERN.matcher(text).replaceAll("");
        return text;
    }

    public static String rainbow(String input) {
        StringBuilder out = new StringBuilder();
        int n = Math.max(1, input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            float hue = (float)i / (float)n;
            java.awt.Color awt = java.awt.Color.getHSBColor(hue, 0.9f, 1.0f);
            String hex = String.format("#%02x%02x%02x", awt.getRed(), awt.getGreen(), awt.getBlue());
            out.append(net.md_5.bungee.api.ChatColor.of(hex));
            out.append(c);
        }
        return out.toString();
    }

    public static boolean isValidVersion(String version) {
        return version.matches("\\d+(\\.\\d+)*"); // Matches version strings like "1", "1.2", "1.2.3", etc.
    }

    public static boolean isVersionLessThan(String version) {
        String serverVersion = Bukkit.getVersion();
        String[] serverParts = serverVersion.split(" ")[2].split("\\.");
        String[] targetParts = version.split("\\.");

        for (int i = 0; i < Math.min(serverParts.length, targetParts.length); i++) {
            if (!isValidVersion(serverParts[i]) || !isValidVersion(targetParts[i])) {
                return false;
            }

            int serverPart = Integer.parseInt(serverParts[i]);
            int targetPart = Integer.parseInt(targetParts[i]);

            if (serverPart < targetPart) {
                return true;
            } else if (serverPart > targetPart) {
                return false;
            }
        }
        return serverParts.length < targetParts.length;
    }

    private static Pattern rgbPat = Pattern.compile("(?:#|0x)(?:[a-f0-9]{3}|[a-f0-9]{6})\\b|(?:rgb|hsl)a?\\([^\\)]*\\)");

    public static String getRGB(String msg) {
        String temp = msg;
        try {

            String status = "none";
            String r = "";
            String g = "";
            String b = "";
            Matcher match = rgbPat.matcher(msg);
            while (match.find()) {
                String color = msg.substring(match.start(), match.end());
                for (char character : msg.substring(match.start(), match.end()).toCharArray()) {
                    switch (character) {
                        case '(':
                            status = "r";
                            continue;
                        case ',':
                            switch (status) {
                                case "r":
                                    status = "g";
                                    continue;
                                case "g":
                                    status = "b";
                                    continue;
                                default:
                                    break;
                            }
                        default:
                            switch (status) {
                                case "r":
                                    r = r + character;
                                    continue;
                                case "g":
                                    g = g + character;
                                    continue;
                                case "b":
                                    b = b + character;
                                    continue;
                            }
                            break;
                    }


                }
                b = b.replace(")", "");
                Color col = new Color(Integer.parseInt(r), Integer.parseInt(g), Integer.parseInt(b));
                temp = temp.replaceFirst("(?:#|0x)(?:[a-f0-9]{3}|[a-f0-9]{6})\\b|(?:rgb|hsl)a?\\([^\\)]*\\)", ChatColor.of(col) + "");
                r = "";
                g = "";
                b = "";
                status = "none";
            }
        } catch (Exception e) {
            return msg;
        }
        return temp;
    }

    public static void runAsync(Runnable task) {
        if (SupremeVouchers.get().isFoliaFound()) {
            Bukkit.getServer().getGlobalRegionScheduler().run(SupremeVouchers.get(), (s) -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(SupremeVouchers.get(), task);
        }
    }

    public static void runMain(Runnable task) {
        if (SupremeVouchers.get().isFoliaFound()) {
            Bukkit.getServer().getGlobalRegionScheduler().execute(SupremeVouchers.get(), task); // global sync task
        } else {
            Bukkit.getScheduler().runTask(SupremeVouchers.get(), task);
        }
    }

    public static void runMainLater(Runnable task, long ticks) {
        if (SupremeVouchers.get().isFoliaFound()) {
            Bukkit.getServer().getGlobalRegionScheduler()
                    .runDelayed(SupremeVouchers.get(), s -> task.run(), ticks);
        } else {
            Bukkit.getScheduler().runTaskLater(SupremeVouchers.get(), task, ticks);
        }
    }

    /**
     * Reads a message from "messages.<path>" and colorizes it.
     * Example: msg("list.give.text") -> reads messages.list.give.text
     */
    public static String msg(String path) {
        String m = SupremeVouchers.get().getConfig().getString("messages." + path, "&cMissing message: " + path);
        m = m.replace("%prefix%", SupremeVouchers.get().getConfig().getString("messages.prefix"));

        return format(m);
    }

    /**
     * Reads a message and replaces placeholders in the form %key% -> value.
     * The returned string is colorized.
     */
    public static String msg(String path, Map<String, String> placeholders) {
        String raw = msg(path);
        if (placeholders != null) {
            raw = formatPlaceholders(raw, placeholders);
        }
        return format(raw);
    }

    public static List<String> listMsg(String path) {
        List<String> list = SupremeVouchers.get().getConfig().getStringList("messages." + path);
        List<String> colored = new ArrayList<>();
        for (String s : list) {
            s = s.replace("%prefix%", SupremeVouchers.get().getConfig().getString("messages.prefix"));
            colored.add(format(s));
        }
        return colored;
    }

    public static String formatPlaceholders(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            result = result.replace("%" + e.getKey() + "%", e.getValue());
        }
        return result;
    }

}
