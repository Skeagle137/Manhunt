package net.skeagle.manhunt.phase;

import net.skeagle.manhunt.config.Settings;
import net.skeagle.manhunt.model.MHBasePhase;
import net.skeagle.manhunt.model.MHManager;
import net.skeagle.manhunt.model.MHState;
import net.skeagle.vrnlib.misc.EventListener;
import net.skeagle.vrnlib.misc.Task;
import net.skeagle.vrnlib.misc.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import static net.skeagle.manhunt.Utils.say;

public class WaitPhase extends MHBasePhase {

    private final int wait = 30;
    private long startIn;
    private long timeElapsed;
    private boolean startCountdown;

    public WaitPhase(MHManager manager) {
        super(manager, MHState.WAITING);
    }

    @Override
    protected void onInit() {
        startIn = (System.currentTimeMillis() / 1000) + wait;
        timeElapsed = 0;
        startCountdown = false;
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.setBedSpawnLocation(Settings.lobbyLocation, true);
            manager.resetPlayerStats(p);
        });

        addListener(new EventListener<>(PlayerJoinEvent.class, e ->
                Task.syncDelayed(() -> manager.fallbackPlayer(e.getPlayer()), 2L)));

        addListener(new EventListener<>(EntityDamageEvent.class, e ->
                e.setCancelled(true)));
    }

    @Override
    protected void onUpdate() {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            previousPhase();
            return;
        }
        else if (Bukkit.getOnlinePlayers().size() < Settings.minPlayers) {
            if (timeElapsed % 30 == 0 && !startCountdown) {
                Bukkit.getOnlinePlayers().forEach(p -> say(p, "&ewaiting for more players to join..."));
            }
            else if (startCountdown) {
                Bukkit.getOnlinePlayers().forEach(p -> say(p, "&cNot enough players to start the game."));
                startCountdown = false;
            }
            timeElapsed++;
            return;
        }
        if (!startCountdown) {
            startIn = (System.currentTimeMillis() / 1000) + wait;
            timeElapsed = 0;
        }
        startCountdown = true;
        //initial waiting countdown
        int timeLeft = (int) (startIn - (System.currentTimeMillis() / 1000));
        if (((timeLeft <= wait && timeLeft % 10 == 0) || timeLeft <= 5) && timeLeft >= 1) {
            Bukkit.getOnlinePlayers().forEach(p -> say(p, "&aStarting in &6" + TimeUtil.timeToMessage(timeLeft) + "&a."));
        }
        else if (timeLeft < 1) {
            nextPhase();
        }
    }
}
