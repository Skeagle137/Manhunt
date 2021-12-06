package net.skeagle.manhunt.model.scoreboard;

import net.skeagle.manhunt.model.MHManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import static net.skeagle.vrnlib.misc.FormatUtils.color;

public class MHScoreboard {

    protected final Scoreboard board;
    protected Objective objective;
    private int end;
    private String display;
    private ChatColor color;
    private final int LINE_CAP = 14;

    public MHScoreboard(String role, String display, ChatColor color) {
        this(role, display, color, 7);
    }

    public MHScoreboard(String role, String display, ChatColor color, int lines) {
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
        this.display = display;
        this.color = color;
        setup(board, role, lines);
    }

    private void setup(Scoreboard board, String role, int lines) {
        objective = board.registerNewObjective("name", "dummy", color("&e&lMANHUNT"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.end = LINE_CAP - lines;
        this.addTeams();

        Team name = board.registerNewTeam("display");
        name.setPrefix(color(display));
        name.setColor(color);

        setBlank();

        setLine(13, "&7Time Left:");
        setLine(10, "&7Your Role:");
        setLine(9, role);
    }

    public void setBlank() {
        for (int i = LINE_CAP; i > end; i--) {
            setLine(i, null);
        }
    }

    public void setLine(int line, String text) {
        if (text == null) {
            text = "";
        }
        board.getTeam(line + "").setPrefix(color(text));
    }

    private void addTeams() {
        for (int i = LINE_CAP; i > end; i--) {
            Team team = this.board.registerNewTeam(i + "");
            team.addEntry(ChatColor.values()[i] + "");
            this.objective.getScore(ChatColor.values()[i] + "").setScore(i);
        }
    }

    public void updateTime(String time) {
        setLine(12, "&e&l" + time);
    }

    public void addPlayer(Player player) {
        Team team = player.getScoreboard().getTeam("display");
        if (team != null) {
            team.removeEntry(player.getName());
        }
        player.setScoreboard(this.board);
        board.getTeam("display").addEntry(player.getName());
    }

}
