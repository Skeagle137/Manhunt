package net.skeagle.manhunt.model;

import net.skeagle.manhunt.Manhunt;
import net.skeagle.manhunt.Settings;
import net.skeagle.manhunt.model.player.HunterPlayer;
import net.skeagle.manhunt.model.player.MHBasePlayer;
import net.skeagle.manhunt.model.player.RunnerPlayer;
import net.skeagle.manhunt.model.scoreboard.MHScoreboard;
import net.skeagle.manhunt.phase.*;
import net.skeagle.manhunt.vote.MHVoteManager;
import net.skeagle.manhunt.world.WorldManager;
import net.skeagle.vrnlib.itemutils.ItemBuilder;
import net.skeagle.vrnlib.itemutils.ItemTrait;
import net.skeagle.vrnlib.itemutils.ItemUtils;
import net.skeagle.vrnlib.misc.EventListener;
import net.skeagle.vrnlib.misc.Task;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;
import java.util.stream.Collectors;

import static net.skeagle.manhunt.Utils.say;
import static net.skeagle.vrncommands.BukkitUtils.color;

public class MHManager {

    /*TODO:
       - vote system to choose hunter or runner (needed impl)
    */

    private final Manhunt plugin;
    private final WorldManager worldManager;
    private final MHVoteManager voteManager;
    private final MHScoreboard runnerBoard, spectatorBoard;
    private final List<MHBasePhase> phases;
    private MHBasePhase currentPhase;
    private MHState gameState;
    private final List<HunterPlayer> hunters;
    private final List<UUID> spectators;
    private RunnerPlayer runner;
    private Task updateTask;
    private int phaseIndex;

    public MHManager(Manhunt plugin, WorldManager worldManager) {
        //setup
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.worldManager.loadWorlds();
        voteManager = new MHVoteManager();
        runnerBoard = new MHScoreboard("&b&lRUNNER", "&8[&bR&8]&r ", ChatColor.AQUA);
        spectatorBoard = new MHScoreboard("&7&lSPECTATOR", "&8[&7S&8]&r ", ChatColor.GRAY);
        phases = new ArrayList<>();
        hunters = new ArrayList<>();
        spectators = new ArrayList<>();

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

        new EventListener<>(PlayerRespawnEvent.class, e -> {
            if (gameState != MHState.WAITING && e.getPlayer().getBedSpawnLocation() == null) {
                Location actual = worldManager.getManhuntWorld().getSpawnLocation();
                actual.setWorld(worldManager.getManhuntWorld());
                e.setRespawnLocation(actual);
            }
        });

        new EventListener<>(PlayerQuitEvent.class, e -> {
            if (!spectators.contains(e.getPlayer().getUniqueId())) {
                e.setQuitMessage(color("&e" + e.getPlayer().getName() + " &7left the game."));
                return;
            }
            e.setQuitMessage(color("&7" + e.getPlayer().getName() + " &8left the spectators."));
        });

        new EventListener<>(PlayerJoinEvent.class, e -> {
            if (gameState != MHState.INGAME && gameState != MHState.ENDED) {
                e.setJoinMessage(color("&e" + e.getPlayer().getName() + " &7joined the game."));
            }
            else {
                e.setJoinMessage(color("&7" + e.getPlayer().getName() + " &8joined the spectators."));
            }

            Task.syncDelayed(() -> {
                Player player = e.getPlayer();
                resetPlayerStats(player);
                if (gameState == MHState.INGAME || gameState == MHState.ENDED) {
                    e.getPlayer().setGameMode(GameMode.SPECTATOR);
                    spectators.add(e.getPlayer().getUniqueId());
                    player.teleport(worldManager.getManhuntWorld().getSpawnLocation());
                    spectatorBoard.addPlayer(player);
                }
                else {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getDiscoveredRecipes().forEach(player::undiscoverRecipe);
                    Iterator<Advancement> it = Bukkit.getServer().advancementIterator();
                    while (it.hasNext()) {
                        AdvancementProgress progress = e.getPlayer().getAdvancementProgress(it.next());
                        progress.getAwardedCriteria().forEach(progress::revokeCriteria);
                    }
                }
            }, 4L);
        });

        new EventListener<>(FoodLevelChangeEvent.class, e -> {
            if (gameState != MHState.INGAME) {
                e.setCancelled(true);
            }
        });

        new EventListener<>(PlayerQuitEvent.class, e -> removeHunter(e.getPlayer()));

        new EventListener<>(AsyncPlayerPreLoginEvent.class, e -> {
            if (gameState == MHState.ENDED) {
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, color("&cThis server is restarting. Please try again later."));
            }
        });

        new EventListener<>(EntityDamageByEntityEvent.class, e -> {
            if (e.getEntity() instanceof Player && gameState != MHState.INGAME) {
                e.setCancelled(true);
            }
        });

        new EventListener<>(EntityDamageEvent.class, e -> {
            if (e.getEntity() instanceof Player && gameState != MHState.STARTING && gameState != MHState.INGAME) {
                e.setCancelled(true);
            }
        });

        new EventListener<>(EntityPickupItemEvent.class, e -> {
            if (e.getEntity() instanceof Player && gameState != MHState.STARTING && gameState != MHState.INGAME) {
                e.setCancelled(true);
            }
        });

        new EventListener<>(PlayerInteractEvent.class, e -> {
            if ((gameState != MHState.STARTING && gameState != MHState.INGAME) && !e.getPlayer().isOp()) {
                e.setCancelled(true);
            }
        });

        //global tracker stuff
        new EventListener<>(PlayerSwapHandItemsEvent.class, e -> {
            if (isTracker(e.getMainHandItem()) || isTracker(e.getOffHandItem())) {
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

        //global spectator stuff

        new EventListener<>(PlayerCommandPreprocessEvent.class, e -> {
            if (spectators.contains(e.getPlayer().getUniqueId())) {
                String s = Settings.allowedSpecCommands.stream().filter(e.getMessage()::startsWith).findFirst().orElse(null);
                if (!e.getPlayer().isOp() && s == null) {
                    e.setCancelled(true);
                    say(e.getPlayer(), "&cYou cannot execute this command as a spectator.");
                }
            }
        });
    }

    public void resetPlayerStats(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        this.resetFood(player);
        player.getInventory().clear();
        player.setFireTicks(0);
        player.setArrowsInBody(0);
        player.setFreezeTicks(0);
        player.setExp(0);
        player.setLevel(0);
        player.setRemainingAir(player.getMaximumAir());
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
    }

    public void resetFood(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(5);
        player.setExhaustion(0);
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
        Bukkit.getOnlinePlayers().forEach(p -> {
            fallbackPlayer(p);
            if (Settings.sendToServerLobby) {
                try {
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteStream);
                    out.writeUTF("Connect");
                    out.writeUTF(Settings.serverLobbyName);
                    p.sendPluginMessage(plugin, "BungeeCord", byteStream.toByteArray());
                    byteStream.close();
                    out.close();
                } catch (Exception e) {
                    p.kickPlayer(color("&cAn error occurred when trying to send you back to the lobby."));
                }
            }
            else {
                p.kickPlayer(color("&6Thanks for playing!"));
            }
        });
        worldManager.deleteAll();
        Task.syncDelayed(plugin.getServer()::shutdown, 10L);
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
        String newTime = String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
        getHunters().forEach(h -> h.getBoard().updateTime(newTime));
        runnerBoard.updateTime(newTime);
        spectatorBoard.updateTime(newTime);
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
