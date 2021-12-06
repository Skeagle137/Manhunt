package net.skeagle.manhunt;

import net.skeagle.manhunt.config.Settings;
import net.skeagle.manhunt.model.MHManager;
import net.skeagle.vrnlib.commandmanager.CommandHook;
import net.skeagle.vrnlib.commandmanager.CommandParser;
import net.skeagle.vrnlib.commandmanager.Messages;
import net.skeagle.vrnlib.misc.EventListener;
import net.skeagle.vrnlib.misc.Task;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static net.skeagle.manhunt.Utils.say;

public final class Manhunt extends JavaPlugin {

    private Settings settings;
    private MHManager manager;

    @Override
    public void onEnable() {
        //settings and messages
        Messages.load(this);
        settings = new Settings(this);
        //manager
        if (Settings.lobbyLocation == null) {
            new EventListener<>(PlayerJoinEvent.class, e ->
                    Task.syncDelayed(() -> {
                        if (e.getPlayer().isOp()) {
                            say(e.getPlayer(), "&cThe lobby location has not been set, so games cannot start." +
                                    "\n&cPlease set the lobby position with /setlobby. Then restart the server.");
                        }
                    }, 4L));
        }
        else {
            manager = new MHManager(this);
        }
        //commands
        new CommandParser(getResource("commands.txt")).parse().register("mhplugin", this);
        //extra things
        if (Settings.sendToServerLobby) {
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }
    }

    @Override
    public void onDisable() {
        settings.get().save();
        if (manager != null) {
            manager.getWorldManager().deleteAll();
        }
    }

    @CommandHook("reload")
    public void onReload(final CommandSender sender) {
        Messages.load(this);
        settings.get().load();
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
    }
}
