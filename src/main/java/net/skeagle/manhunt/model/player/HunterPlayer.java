package net.skeagle.manhunt.model.player;

import net.skeagle.manhunt.model.MHManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static net.skeagle.manhunt.Utils.sayActionBar;

public class HunterPlayer extends MHBasePlayer {

    public HunterPlayer(MHManager manager, Player player) {
        super(manager, player);
    }

    @Override
    public void update() {
        ItemStack item = this.getPlayer().getInventory().getItem(8);
        if (!getManager().isTracker(item) || item == null)
            return;
        Location runnerLoc = getManager().getRunner().getPlayer().getLocation();
        getPlayer().setCompassTarget(lastRunnerLocation() != null ? lastRunnerLocation() : runnerLoc);
        if (getManager().isTracker(this.getPlayer().getInventory().getItemInMainHand())) {
            boolean sameWorld = this.getPlayer().getWorld().getEnvironment() == getManager().getRunner().getPlayer().getWorld().getEnvironment();
            Location trackedLocation = sameWorld ? runnerLoc : (lastRunnerLocation() != null ? lastRunnerLocation() : null);
            int i = trackedLocation != null ? (int) this.getPlayer().getLocation().distance(trackedLocation) : -1;
            String s;
            if (i < 0)
                s = getManager().getRunner().getPlayer().getName() + " has no last portal location.";
            else if (!sameWorld)
                s = "Last portal " + getManager().getRunner().getPlayer().getName() + " entered is &e&l" + i + "m&b away";
            else
                s = getManager().getRunner().getPlayer().getName() + " is &e&l" + i + "m&b away";
            sayActionBar(this.getPlayer(), "&b" + s);
        }
    }

    private Location lastRunnerLocation() {
        RunnerPlayer runner = this.getManager().getRunner();
        if (this.getPlayer().getWorld().getEnvironment() != runner.getPlayer().getWorld().getEnvironment()) {
            return runner.getLastPortal();
        }
        return runner.getPlayer().getLocation();
    }
}
