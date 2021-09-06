package net.skeagle.manhunt.config;

import net.skeagle.manhunt.Manhunt;
import net.skeagle.manhunt.Utils;
import net.skeagle.vrnlib.configmanager.ConfigManager;
import net.skeagle.vrnlib.configmanager.annotations.ConfigValue;
import org.bukkit.Location;

import java.util.List;

public class Settings {
    @ConfigValue("world-name")
    public static String worldName = "manhunt";
    @ConfigValue("min-players")
    public static int minPlayers = 2;
    @ConfigValue("max-time-minutes")
    public static int maxTime = 120;
    @ConfigValue("restart-time-seconds")
    public static int restartTime = 30;
    @ConfigValue("lobby-location")
    public static Location lobbyLocation;
    @ConfigValue("start-area-size")
    public static int startAreaDiameter = 7;
    @ConfigValue("send-to-server-lobby")
    public static boolean sendToServerLobby = false;
    @ConfigValue("spectators-allowed-commands")
    public static List<String> allowedSpecCommands = ConfigManager.list(String.class, "lobby", "hub", "server");

    private ConfigManager config;

    public Settings(final Manhunt plugin) {
        config = new ConfigManager(plugin).addConverter(Location.class, Utils::deserializeLocation, Utils::serializeLocation).register(this).saveDefaults().load();
    }

    private Settings() {
    }

    public ConfigManager get() {
        return config;
    }
}
