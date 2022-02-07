package net.skeagle.manhunt;

import net.skeagle.manhunt.model.MHManager;
import net.skeagle.manhunt.world.WorldManager;
import net.skeagle.vrncommands.BukkitCommandParser;
import net.skeagle.vrncommands.BukkitCommandRegistry;
import net.skeagle.vrncommands.BukkitMessages;
import net.skeagle.vrncommands.CommandHook;
import net.skeagle.vrnlib.config.ConfigManager;
import net.skeagle.vrnlib.misc.EventListener;
import net.skeagle.vrnlib.misc.Task;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static net.skeagle.manhunt.Utils.say;

public final class Manhunt extends JavaPlugin {

    private ConfigManager config;
    private WorldManager worldManager;
    private boolean loaded = false;

    @Override
    public void onEnable() {
        //settings and messages
        BukkitMessages.load(this);
        config = ConfigManager.create(this).addConverter(Location.class, Utils::deserializeLocation, Utils::serializeLocation).target(Settings.class).saveDefaults().load();
        worldManager = new WorldManager(this);
        if (Settings.lobbyLocation == null) {
            new EventListener<>(PlayerJoinEvent.class, e ->
                    Task.syncDelayed(() -> {
                        if (e.getPlayer().isOp()) {
                            say(e.getPlayer(), "&cThe lobby location has not been set, so games cannot start." +
                                    "\n&cPlease set the lobby position with /setlobby, then the game will enable.");
                        }
                    }, 4L));
        }
        else {
            load();
        }
        //commands
        new BukkitCommandParser(getResource("commands.txt")).parse().register(new BukkitCommandRegistry(this), "manhunt", this);
        //extra things
        if (Settings.sendToServerLobby) {
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }
    }

    @Override
    public void onDisable() {
        config.save();
    }

    public void load() {
        new MHManager(this, worldManager);
        loaded = true;
    }

    @CommandHook("reload")
    public void onReload(final CommandSender sender) {
        BukkitMessages.load(this);
        config.reload();
        say(sender, "&aConfig and messages reloaded.");
    }

    @CommandHook("setlobby")
    public void onSetLobby(Player player) {
        if (player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
            say(player, "&cThe lobby cannot be in the manhunt world.");
            return;
        }
        Settings.lobbyLocation = player.getLocation();
        say(player, "&aUpdated the lobby position. Players will spawn here when they join and when a game has ended.");
        if (!loaded) {
            load();
        }
    }
}
