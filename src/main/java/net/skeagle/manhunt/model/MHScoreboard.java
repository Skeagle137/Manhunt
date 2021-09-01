package net.skeagle.manhunt.model;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import static net.skeagle.vrnlib.misc.FormatUtils.color;

public class MHScoreboard {

    private final Scoreboard board;

    MHScoreboard(String role, String displayName, ChatColor color) {
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
        setup(board, role, displayName, color);
    }

    private void setup(Scoreboard board, String s, String displayName, ChatColor color) {
        Objective objective = board.registerNewObjective("name", "dummy", color("&e&lMANHUNT"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Team name = board.registerNewTeam("displayName");
        name.setPrefix(color(displayName));
        name.setColor(color);
        Team time = board.registerNewTeam("gameTime");
        Team role = board.registerNewTeam("role");
        time.addEntry(color("&0&r"));
        role.addEntry(color("&1&r"));
        role.setPrefix(color(s));

        objective.getScore(color("&r&a&r")).setScore(7);
        objective.getScore(color("&7Time Left:")).setScore(6);
        objective.getScore(color("&0&r")).setScore(5);
        objective.getScore(color("&r&b&r")).setScore(4);
        objective.getScore(color("&7Your role:")).setScore(3);
        objective.getScore(color("&1&r")).setScore(2);
        objective.getScore(color("&r&c&r")).setScore(1);
    }

    public void updateTime(String time) {
        getDisplayTime().setPrefix(color("&e&l" + time));
    }

    public Scoreboard getBoard() {
        return board;
    }

    public Team getTeam() {
        return board.getTeam("displayName");
    }

    public Team getDisplayTime() {
        return board.getTeam("gameTime");
    }

    public Team getDisplayRole() {
        return board.getTeam("role");
    }

}
