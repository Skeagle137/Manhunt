package net.skeagle.manhunt;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.skeagle.vrncommands.BukkitUtils.color;

public class Utils {

    public static void say(CommandSender cs, String... message) {
        if (cs == null) return;
        for (String msg : message)
            cs.sendMessage(color(msg));
    }

    public static void sayActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(msg)));
    }

    public static String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return null;
        return loc.getWorld().getName() + " " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " " + loc.getYaw() + " " + loc.getPitch();
    }

    public static Location deserializeLocation(String s) {
        if (s == null) return null;
        String[] split = s.split(" ");
        if (split.length != 6)
            return null;
        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]),
                Float.parseFloat(split[4]), Float.parseFloat(split[5]));
    }
}
