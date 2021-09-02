package net.skeagle.manhunt.phase;

import net.skeagle.manhunt.config.Settings;
import net.skeagle.manhunt.model.MHBasePhase;
import net.skeagle.manhunt.model.MHManager;
import net.skeagle.manhunt.model.MHScoreboard;
import net.skeagle.manhunt.model.MHState;
import net.skeagle.manhunt.model.player.MHBasePlayer;
import net.skeagle.manhunt.model.player.RunnerPlayer;
import net.skeagle.vrnlib.misc.EventListener;
import net.skeagle.vrnlib.misc.Task;
import net.skeagle.vrnlib.misc.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.*;

import java.util.Random;

import static net.skeagle.manhunt.Utils.say;
import static net.skeagle.vrnlib.misc.FormatUtils.color;

public class StartingPhase extends MHBasePhase {

    private final Random rand;
    private long startIn;
    private double particleOffset;
    private boolean huntersChosen;
    private World world;
    private Location[] startRegion;
    private final int countdown = 30;

    public StartingPhase(MHManager manager) {
        super(manager, MHState.STARTING);
        rand = new Random();
    }

    @Override
    protected void onInit() {
        particleOffset = 0;
        startIn = (System.currentTimeMillis() / 1000) + countdown;
        huntersChosen = false;
        world = manager.getWorldManager().getManhuntWorld();
        setupPlayers();
        chooseRunner();

        addListener(new EventListener<>(PlayerJoinEvent.class, e -> {
            teleportPlayerToMHWorld(e.getPlayer());
            addHunter(e.getPlayer());
        }));

        addListener(new EventListener<>(PlayerQuitEvent.class, e -> {
            if (Bukkit.getOnlinePlayers().size() <= 2) {
                previousPhase();
            } else if (e.getPlayer() == manager.getRunner().getPlayer()) {
                Bukkit.getOnlinePlayers().forEach(p -> say(p, "The assigned speedrunner has left; choosing another player."));
                startIn = (System.currentTimeMillis() / 1000) + countdown;
                chooseRunner();
            }
        }));

        addListener(new EventListener<>(PlayerMoveEvent.class, e -> {
            MHBasePlayer player;
            player = manager.isHunter(e.getPlayer()) ? manager.getHunter(e.getPlayer()) : manager.getRunner();
            if (player == null)
                return;
            if (player.isReleased())
                return;
            Location to = e.getTo();
            if (to == null || startRegion == null)
                return;
            if (to.getBlock().getX() < startRegion[0].getBlock().getX() && to.getBlock().getX() > startRegion[1].getBlock().getX() &&
                    to.getBlock().getZ() < startRegion[0].getBlock().getZ() && to.getBlock().getZ() > startRegion[1].getBlock().getZ()) {
                return;
            }
            e.getPlayer().teleport(e.getPlayer().getBedSpawnLocation());
        }));

        addListener(new EventListener<>(EntityDamageEvent.class, e -> {
            if (!(e.getEntity() instanceof Player))
                return;
            if (isHunterOrNotReleased((Player) e.getEntity())) {
                e.setCancelled(true);
            }
        }));

        addListener(new EventListener<>(EntityPickupItemEvent.class, e -> {
            if (!(e.getEntity() instanceof Player))
                return;
            if (isHunterOrNotReleased((Player) e.getEntity())) {
                e.setCancelled(true);
            }
        }));

        addListener(new EventListener<>(BlockBreakEvent.class, e -> {
            if (isHunterOrNotReleased(e.getPlayer())) {
                e.setCancelled(true);
            }
        }));

        addListener(new EventListener<>(PlayerInteractEvent.class, e -> {
            if (isHunterOrNotReleased(e.getPlayer())) {
                e.setCancelled(true);
            }
        }));

        Task.syncDelayed(() -> manager.getMHBasePlayers().forEach(bp -> manager.setReleased(bp, false)), 4L);

        addTask(Task.syncRepeating(() -> {
            manager.getMHBasePlayers().forEach(this::drawParticles);
            this.particleOffset += 0.5;
        }, 0L, 4L));

        manager.updateDisplayTime(Settings.maxTime * 60L);
    }

    @Override
    protected void onUpdate() {
        int timeLeft = (int) (startIn - (System.currentTimeMillis() / 1000));
        if (timeLeft > 15 && (timeLeft <= 20 || timeLeft % 10 == 0)) {
            manager.getMHPlayers().forEach(p -> say(p, "&eRunner released for head start in &a" + TimeUtil.timeToMessage(timeLeft - 15) + "&e."));
        } else if (timeLeft <= 15 && !manager.getRunner().isReleased()) {
            manager.getMHPlayers().forEach(p -> say(p, "&aThe speedrunner has been released for a head start."));
            manager.setReleased(manager.getRunner(), true);
        }

        if ((timeLeft % 5 == 0 || timeLeft <= 5) && timeLeft > 0 && timeLeft <= 15) {
            manager.getMHPlayers().forEach(p -> say(p, "&cHunters released in &e" + timeLeft + " seconds."));
        }
        else if (timeLeft < 1) {
            manager.getHunters().forEach(h -> manager.setReleased(h, true));
            manager.getMHPlayers().forEach(p -> say(p, "&cHunters have been released."));
            nextPhase();
        }
    }

    @Override
    protected void onEnd(MHState newState) {
        if (newState != MHState.WAITING)
            return;
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.teleport(Settings.lobbyLocation);
            say(p, "&cYou have been brought back to the lobby since there are not enough players to start the game.");
        });
        manager.clearPlayers();
    }

    private boolean isHunterOrNotReleased(Player p) {
        return manager.isHunter(p) || !manager.getRunner().isReleased();
    }

    private void setupPlayers() {
        double d = (double) Settings.startAreaDiameter / 2;
        Location loc = world.getSpawnLocation();
        this.startRegion = new Location[]{loc.clone().add(d, 0, d), loc.clone().add(-d, 0, -d),
                loc.clone().add(-d, 0, d), loc.clone().add(d, 0, -d)};
        Bukkit.getOnlinePlayers().forEach(this::teleportPlayerToMHWorld);
    }

    private void teleportPlayerToMHWorld(Player player) {
        Location loc = world.getSpawnLocation();
        loc.setWorld(world);
        player.teleport(loc);
        player.setBedSpawnLocation(loc, true);
    }

    private void chooseRunner() {
        int i = rand.ints(0, Bukkit.getOnlinePlayers().size()).findFirst().getAsInt();
        RunnerPlayer runner = new RunnerPlayer(manager, Bukkit.getOnlinePlayers().stream().toList().get(i));
        runner.getPlayer().sendTitle(color("&b&lRUNNER"), color("&eYour role has been assigned"), 5, 160, 40);
        manager.setRunner(runner);
        setScoreboard(runner.getPlayer(), manager.getRunnerBoard());
        //remove if previously a hunter
        manager.removeHunter(runner.getPlayer());
        if (!huntersChosen) {
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p != runner.getPlayer()) {
                    addHunter(p);
                }
            });
            huntersChosen = true;
        }
        Bukkit.getOnlinePlayers().forEach(p -> say(p, "&e" + runner.getPlayer().getName() + " has been chosen to be the runner."));
    }

    private void drawParticles(MHBasePlayer player) {
        if (player.isReleased())
            return;
        double offset = this.particleOffset % Settings.startAreaDiameter;
        for (double d = player.getPlayer().getLocation().getBlockY() - 20; d <= player.getPlayer().getLocation().getBlockY() + 30; d += 2.5) {
            spawnParticle(player.getPlayer(), startRegion[0].getX(), d, startRegion[0].getZ() - offset);
            spawnParticle(player.getPlayer(), startRegion[1].getX(), d, startRegion[1].getZ() + offset);
            spawnParticle(player.getPlayer(), startRegion[2].getX() + offset, d, startRegion[2].getZ());
            spawnParticle(player.getPlayer(), startRegion[3].getX() - offset, d, startRegion[3].getZ());
        }
    }

    private void spawnParticle(Player player, double x, double y, double z) {
        player.spawnParticle(Particle.SOUL_FIRE_FLAME, x, y, z, 1, 0, 0, 0, 0.01);
    }

    private void addHunter(Player player) {
        player.sendTitle(color("&c&lHUNTER"), color("&eYour role has been assigned"), 5, 160, 40);
        setScoreboard(player, manager.getHunterBoard());
        manager.addHunter(player);
        manager.setReleased(manager.getHunter(player), false);
    }

    private void setScoreboard(Player player, MHScoreboard board) {
        player.setScoreboard(board.getBoard());
        board.getTeam().addEntry(player.getName());
    }
}
