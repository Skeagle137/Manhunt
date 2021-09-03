package net.skeagle.manhunt.model.player;

import net.skeagle.manhunt.model.MHManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RunnerPlayer extends MHBasePlayer {

    private Location lastPortal;
    private boolean enteredNether;
    private boolean enteredEnd;

    public RunnerPlayer(MHManager manager, Player player) {
        super(manager, player);
    }

    public Location getLastPortal() {
        return lastPortal;
    }

    public void setLastPortal(Location lastPortal) {
        this.lastPortal = lastPortal;
    }

    public boolean hasEnteredNether() {
        return enteredNether;
    }

    public boolean hasEnteredEnd() {
        return enteredEnd;
    }
}
