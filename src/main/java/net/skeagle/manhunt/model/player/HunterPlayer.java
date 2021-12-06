package net.skeagle.manhunt.model.player;

import net.skeagle.manhunt.model.MHManager;
import net.skeagle.manhunt.model.scoreboard.HunterScoreboard;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

import static net.skeagle.manhunt.Utils.sayActionBar;

public class HunterPlayer extends MHBasePlayer {

    private final HunterScoreboard board;

    public HunterPlayer(MHManager manager, Player player) {
        super(manager, player);
        this.board = new HunterScoreboard();
        this.board.addPlayer(player);
    }

    @Override
    public void update() {
        ItemStack item = this.getPlayer().getInventory().getItem(8);
        if (!getManager().isTracker(item) || item == null)
            return;
        Location runnerLoc = getManager().getRunner().getPlayer().getLocation();
        if (getPlayer().getWorld().getEnvironment() != World.Environment.NETHER) {
            getPlayer().setCompassTarget(lastRunnerLocation() != null ? lastRunnerLocation() : runnerLoc);
        }
        else {
            ItemMeta meta = item.getItemMeta();
            CompassMeta compassMeta = (CompassMeta) meta;
            compassMeta.setLodestoneTracked(false);
            compassMeta.setLodestone(lastRunnerLocation() != null ? lastRunnerLocation() : runnerLoc);
            item.setItemMeta(compassMeta);
            if (getPlayer().getGameMode() != GameMode.CREATIVE) {
                getPlayer().updateInventory();
            }
        }
        if (getManager().isTracker(this.getPlayer().getInventory().getItemInMainHand())) {
            boolean sameWorld = this.getPlayer().getWorld().getEnvironment() == getManager().getRunner().getPlayer().getWorld().getEnvironment();
            Location trackedLocation = sameWorld ? runnerLoc : (lastRunnerLocation() != null ? lastRunnerLocation() : null);
            int i = trackedLocation != null ? (int) this.getPlayer().getLocation().distance(trackedLocation) : -1;
            String s;
            if (i < 0)
                s = getManager().getRunner().getPlayer().getName() + " cannot be tracked here.";
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

    public Map<HunterPlayer, Integer> getHuntersNear() {
        Map<HunterPlayer, Integer> sorted = new LinkedHashMap<>();
        this.getManager().getHunters().forEach(h -> sorted.put(h, this.getPlayer().getWorld().getEnvironment() == h.getPlayer().getWorld().getEnvironment() ?
                (int) h.getPlayer().getLocation().distance(this.getPlayer().getLocation()) : -1));
        return sorted.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue))
                .filter(h -> !this.equals(h.getKey())).limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public HunterScoreboard getBoard() {
        return board;
    }
}
