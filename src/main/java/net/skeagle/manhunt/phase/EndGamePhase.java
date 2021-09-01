package net.skeagle.manhunt.phase;

import net.skeagle.manhunt.config.Settings;
import net.skeagle.manhunt.model.MHBasePhase;
import net.skeagle.manhunt.model.MHManager;
import net.skeagle.manhunt.model.MHState;
import net.skeagle.vrnlib.misc.EventListener;
import net.skeagle.vrnlib.misc.Task;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class EndGamePhase extends MHBasePhase {

    public EndGamePhase(MHManager manager) {
        super(manager, MHState.ENDED);
    }

    @Override
    protected void onInit() {
        Task.syncDelayed(() -> {
            Bukkit.getOnlinePlayers().forEach(p -> p.getInventory().clear());
            nextPhase();
        }, Settings.restartTime * 20L);

        addListener(new EventListener<>(EntityDamageEvent.class, e -> {
            if (e instanceof Player) {
                e.setCancelled(true);
            }
        }));
    }
}
