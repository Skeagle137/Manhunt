package net.skeagle.manhunt.model.scoreboard;

import net.skeagle.manhunt.model.player.HunterPlayer;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Team;

import java.util.Map;

import static net.skeagle.vrnlib.misc.FormatUtils.color;

public class HunterScoreboard extends MHScoreboard {

    public HunterScoreboard() {
        super("&c&lHUNTER", "&8[&cH&8]&r ", ChatColor.RED, 14);
        setLine(7, "&d&lClosest Hunters:");
    }

    public void updateHunters(Map<HunterPlayer, Integer> hunters) {
        int i = 6;
        for (HunterPlayer hunter : hunters.keySet()) {
            Team team = board.getTeam(i + "");
            team.setPrefix(color("&e" + hunter.getPlayer().getName() + "&7: "));
            team.setSuffix(color("&b" + (hunters.get(hunter) > -1 ? hunters.get(hunter) + "m" : "&cN/A")));
            i--;
        }
        while (i > 1) {
            setLine(i, null);
            i--;
        }
    }

}
