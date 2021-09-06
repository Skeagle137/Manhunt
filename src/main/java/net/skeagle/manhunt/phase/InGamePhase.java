package net.skeagle.manhunt.phase;

import net.skeagle.manhunt.config.Settings;
import net.skeagle.manhunt.model.*;
import net.skeagle.manhunt.model.player.HunterPlayer;
import net.skeagle.vrnlib.misc.EventListener;
import net.skeagle.vrnlib.misc.Task;
import net.skeagle.vrnlib.misc.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import static net.skeagle.manhunt.Utils.say;

public class InGamePhase extends MHBasePhase {

    private int maxInGameTime;
    private int time;

    public InGamePhase(MHManager manager) {
        super(manager, MHState.INGAME);
    }

    @Override
    protected void onInit() {
        maxInGameTime = Settings.maxTime * 60;
        time = 0;
        manager.getHunters().forEach(h -> h.getPlayer().getInventory().setItem(8, manager.getTrackerItem()));

        addListener(new EventListener<>(PlayerQuitEvent.class, e -> {
            if (e.getPlayer() == manager.getRunner().getPlayer()) {
                endGame(MHWinner.HUNTERS, MHEndReason.RUNNER_QUIT);
            }
            else if (manager.getHunters().isEmpty()) {
                endGame(MHWinner.RUNNER, MHEndReason.HUNTERS_QUIT);
            }
        }));

        addListener(new EventListener<>(PlayerRespawnEvent.class, e -> {
            if (e.getPlayer() != manager.getRunner().getPlayer()) {
                Task.syncDelayed(() -> e.getPlayer().getInventory().setItem(8, manager.getTrackerItem()), 2L);
            }
        }));

        addListener(new EventListener<>(PlayerPortalEvent.class, e -> {
            if (e.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
                if (e.getFrom().getWorld().getEnvironment() == World.Environment.NETHER) {
                    Location to = e.getFrom().multiply(8);
                    to.setWorld(manager.getWorldManager().getManhuntWorld());
                    e.setTo(to);
                }
                else {
                    e.getTo().setWorld(manager.getWorldManager().getManhuntNether());
                }
            }
            else if (e.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                e.getTo().setWorld(manager.getWorldManager().getManhuntEnd());
            }
            if (e.getPlayer() != manager.getRunner().getPlayer())
                return;
            if (e.getTo() != null) {
                if (e.getTo().getWorld() != e.getFrom().getWorld()) {
                    manager.getRunner().setLastPortal(e.getFrom());
                }
            }
        }));

        addListener(new EventListener<>(PlayerDeathEvent.class, e -> {
            if (e.getEntity() == manager.getRunner().getPlayer()) {
                endGame(MHWinner.HUNTERS, MHEndReason.RUNNER_KILLED);
            }
        }));

        addListener(new EventListener<>(EntityDeathEvent.class, e -> {
            if (e.getEntity() instanceof EnderDragon) {
                endGame(MHWinner.RUNNER, MHEndReason.DRAGON_KILLED);
            }
        }));

        addTask(Task.syncRepeating(() ->
                manager.getHunters().forEach(HunterPlayer::update), 0L, 4L));
    }

    @Override
    protected void onUpdate() {
        manager.updateDisplayTime(maxInGameTime - time);
        if (time >= maxInGameTime) {
            endGame(MHWinner.DRAW, MHEndReason.OUT_OF_TIME);
            return;
        }
        time++;
    }

    private void endGame(MHWinner winner, MHEndReason reason) {
        String s = reason.get();
        if (reason == MHEndReason.RUNNER_KILLED) {
            if (manager.getRunner().getPlayer().getKiller() != null) {
                s = s.replaceAll("%hunter%", manager.getRunner().getPlayer().getKiller().getName());
            }
            else {
                s = MHEndReason.RUNNER_DIED.get();
            }
        }
        if (reason == MHEndReason.DRAGON_KILLED || reason == MHEndReason.RUNNER_DIED || reason == MHEndReason.RUNNER_KILLED || reason == MHEndReason.RUNNER_QUIT) {
            s = s.replaceAll("%runner%", manager.getRunner().getPlayer().getName());
        }
        if (reason == MHEndReason.OUT_OF_TIME) {
            s = s.replaceAll("%time%", TimeUtil.timeToMessage(maxInGameTime));
        }
        String finalResult = s;
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendTitle(winner.getTitle(), finalResult, 10, 100, 80);
            say(p, "&eRestarting in &a" + TimeUtil.timeToMessage(Settings.restartTime) + "&e.");
        });
        nextPhase();
    }
}
