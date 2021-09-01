package net.skeagle.manhunt.phase;

import net.skeagle.manhunt.model.MHBasePhase;
import net.skeagle.manhunt.model.MHManager;
import net.skeagle.manhunt.model.MHState;
import net.skeagle.vrnlib.misc.EventListener;
import net.skeagle.vrnlib.misc.Task;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerJoinEvent;

public class IdlePhase extends MHBasePhase {

    public IdlePhase(MHManager manager) {
        super(manager, MHState.IDLE);
    }

    @Override
    protected void onInit() {
        addListener(new EventListener<>(PlayerJoinEvent.class, e -> {
            Task.syncDelayed(() -> manager.fallbackPlayer(e.getPlayer()), 2L);
            if (Bukkit.getOnlinePlayers().size() == 1) {
                nextPhase();
            }
        }));
    }
}
