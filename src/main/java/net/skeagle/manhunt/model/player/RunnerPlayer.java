package net.skeagle.manhunt.model.player;

import net.skeagle.manhunt.model.MHManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RunnerPlayer extends MHBasePlayer {

    private Location lastPortal;

    public RunnerPlayer(MHManager manager, Player player) {
        super(manager, player);
    }

    public Location getLastPortal() {
        return lastPortal;
    }

    public void setLastPortal(Location lastPortal) {
        this.lastPortal = lastPortal;
    }
}
