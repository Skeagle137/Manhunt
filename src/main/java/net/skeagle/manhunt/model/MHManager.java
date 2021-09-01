package net.skeagle.manhunt.model;

import net.skeagle.manhunt.Manhunt;
import net.skeagle.manhunt.config.Settings;
import net.skeagle.manhunt.model.player.HunterPlayer;
import net.skeagle.manhunt.model.player.MHBasePlayer;
import net.skeagle.manhunt.model.player.RunnerPlayer;
import net.skeagle.manhunt.phase.*;
import net.skeagle.manhunt.vote.MHVoteManager;
import net.skeagle.manhunt.world.WorldManager;
import net.skeagle.vrnlib.itemutils.ItemBuilder;
import net.skeagle.vrnlib.itemutils.ItemTrait;
import net.skeagle.vrnlib.itemutils.ItemUtils;
import net.skeagle.vrnlib.misc.EventListener;
import net.skeagle.vrnlib.misc.Task;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

import static net.skeagle.vrnlib.misc.FormatUtils.color;

public class MHManager {

    /*TODO:
       - safe tp players to land
       - 3+ players bug testing
       - vote system to choose hunter or runner (needed impl)
       */

    private final Manhunt plugin;
    private final WorldManager worldManager;
    private final MHVoteManager voteManager;
    private final MHScoreboard runnerBoard, hunterBoard;
    private final List<MHBasePhase> phases;
    private MHBasePhase currentPhase;
    private MHState gameState;
    private final List<HunterPlayer> hunters;
    private RunnerPlayer runner;
    private Task updateTask;
    private int phaseIndex;

    public MHManager(Manhunt plugin) {
        //setup
        this.plugin = plugin;
        worldManager = new WorldManager();
        voteManager = new MHVoteManager();
        runnerBoard = new MHScoreboard("&b&lRUNNER", "&8[&bR&8]&r", ChatColor.AQUA);
        hunterBoard = new MHScoreboard("&c&lHUNTER", "&8[&cH&8]&r", ChatColor.RED);
        phases = new ArrayList<>();
        hunters = new ArrayList<>();

        //add phases
        phases.add(new IdlePhase(this));
        phases.add(new WaitPhase(this));
        phases.add(new StartingPhase(this));
        phases.add(new InGamePhase(this));
        phases.add(new EndGamePhase(this));

        //start in idle phase
        gameState = MHState.IDLE;
        startCurrentPhase();

        //global events
        new EventListener<>(PlayerQuitEvent.class, e -> removeHunter(e.getPlayer()));

        new EventListener<>(AsyncPlayerPreLoginEvent.class, e -> {
            if (gameState == MHState.INGAME || gameState == MHState.ENDED) {
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, color("&cThis game is in progress."));
            }
        });

        //global tracker stuff
        new EventListener<>(PlayerSwapHandItemsEvent.class, e -> {
            if (isTracker(e.getMainHandItem())) {
                e.setCancelled(true);
            }
        });

        new EventListener<>(PlayerDeathEvent.class, e -> e.getDrops().removeIf(this::isTracker));

        new EventListener<>(PlayerDropItemEvent.class, e -> {
            if (isTracker(e.getItemDrop().getItemStack())) {
                e.setCancelled(true);
            }
        });

        new EventListener<>(InventoryClickEvent.class, e -> {
            if (e.getClick() == ClickType.NUMBER_KEY && (e.getHotbarButton() == 8 || isTracker(e.getCurrentItem()))) {
                e.setCancelled(true);
            }
            if (isTracker(e.getCurrentItem()) || isTracker(e.getCursor())) {
                e.setCancelled(true);
            }
        });

        new EventListener<>(InventoryMoveItemEvent.class, e -> {
            if (isTracker(e.getItem())) {
                e.setCancelled(true);
            }
        });
    }

    public ItemStack getTrackerItem() {
        return new ItemBuilder(Material.COMPASS).setName("&b&lTracker").setCount(1)
                .addEnchant(Enchantment.MENDING, 1).addItemFlags(ItemFlag.HIDE_ENCHANTS)
                .setLore("&dPoints in the direction of the speedrunner", "&dor the last portal they entered.");
    }

    public boolean isTracker(ItemStack item) {
        return ItemUtils.compare(item, this.getTrackerItem(), ItemTrait.ENCHANTMENTS, ItemTrait.LORE, ItemTrait.TYPE);
    }

    public void fallbackPlayer(Player player) {
        player.teleport(Settings.lobbyLocation);
        player.getInventory().clear();
    }

    public Task getUpdateTask() {
        return updateTask;
    }

    public void startUpdateTask() {
        this.updateTask = Task.syncRepeating(() -> {
            if (currentPhase != null) {
                currentPhase.onUpdate();
            }
        }, 0L, 20L);
    }

    private void resetAndClose() {
        Bukkit.getOnlinePlayers().forEach(p -> p.kickPlayer(color("&6Thanks for playing!")));
        worldManager.deleteAll();
        plugin.getServer().shutdown();
    }

    public Manhunt getPlugin() {
        return plugin;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public MHVoteManager getVoteManager() {
        return voteManager;
    }

    public MHScoreboard getRunnerBoard() {
        return runnerBoard;
    }

    public MHScoreboard getHunterBoard() {
        return hunterBoard;
    }

    public MHState getState() {
        return gameState;
    }

    public void setState(MHState state) {
        this.gameState = state;
    }

    public List<MHBasePhase> getPhases() {
        return phases;
    }

    public void nextPhase() {
        phaseIndex++;
        if (phaseIndex == phases.size()) {
            resetAndClose();
            return;
        }
        startCurrentPhase();
    }

    public void previousPhase() {
        phaseIndex--;
        if (phaseIndex < 0) {
            phaseIndex = 0;
        }
        startCurrentPhase();
    }

    public void startCurrentPhase() {
        currentPhase = phases.get(phaseIndex);
        currentPhase.startPhase();
    }

    public MHBasePhase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(MHBasePhase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public void setReleased(MHBasePlayer player, boolean released) {
        player.setReleased(released);
    }

    public void clearPlayers() {
        hunters.clear();
        runner = null;
    }

    public void updateDisplayTime(long time) {
        long hours = time % 31536000L % 86400L / 3600L;
        long minutes = time % 31536000L % 86400L % 3600L / 60L;
        long seconds = time % 31536000L % 86400L % 3600L % 60L;
        String s = String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
        hunterBoard.updateTime(s);
        runnerBoard.updateTime(s);
    }

    public List<HunterPlayer> getHunters() {
        return hunters;
    }

    public HunterPlayer getHunter(Player player) {
        return hunters.stream().filter(h -> h.getPlayer().getUniqueId().equals(player.getUniqueId())).findFirst().orElse(null);
    }

    public boolean isHunter(Player player) {
        return getHunter(player) != null;
    }

    public void addHunter(Player... players) {
        hunters.addAll(Arrays.stream(players).map(p -> new HunterPlayer(this, p)).collect(Collectors.toList()));
    }

    public void removeHunter(Player... players) {
        hunters.removeIf(h -> Arrays.stream(players).toList().contains(h.getPlayer()));
    }

    public void setRunner(RunnerPlayer runner) {
        this.runner = runner;
    }

    public RunnerPlayer getRunner() {
        return runner;
    }

    public List<MHBasePlayer> getMHBasePlayers() {
        List<MHBasePlayer> players = new ArrayList<>(hunters);
        players.add(runner);
        return players;
    }

    public List<Player> getMHPlayers() {
        return this.getMHBasePlayers().stream().map(MHBasePlayer::getPlayer).collect(Collectors.toList());
    }
}
