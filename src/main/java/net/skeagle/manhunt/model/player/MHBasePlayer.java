package net.skeagle.manhunt.model.player;

import net.skeagle.manhunt.model.MHManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class MHBasePlayer {

    private final Player player;
    private MHManager manager;
    private boolean released;

    public MHBasePlayer(MHManager manager, Player player) {
        this.player = player;
        this.manager = manager;
    }

    public MHBasePlayer(UUID uuid) {
        this.player = Bukkit.getPlayer(uuid);
    }

    public Player getPlayer() {
        return player;
    }

    public MHManager getManager() {
        return manager;
    }

    public void update() {

    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }
}
