package net.skeagle.manhunt;

import net.skeagle.vrnlib.config.annotations.ConfigName;
import org.bukkit.Location;

import java.util.List;

public class Settings {

    @ConfigName("world-name")
    public static String worldName = "manhunt";
    @ConfigName("min-players")
    public static int minPlayers = 2;
    @ConfigName("max-time-minutes")
    public static int maxTime = 120;
    @ConfigName("restart-time-seconds")
    public static int restartTime = 30;
    @ConfigName("lobby-location")
    public static Location lobbyLocation;
    @ConfigName("start-area-size")
    public static int startAreaDiameter = 7;
    @ConfigName("send-to-server-lobby")
    public static boolean sendToServerLobby = false;
    @ConfigName("server-lobby-name")
    public static String serverLobbyName = "lobby";
    @ConfigName("world-border-radius")
    public static int worldBorder = 8000;
    @ConfigName("spectators-allowed-commands")
    public static List<String> allowedSpecCommands = List.of("lobby", "hub", "server");

}
