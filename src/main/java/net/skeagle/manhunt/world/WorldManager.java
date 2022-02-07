package net.skeagle.manhunt.world;

import net.skeagle.manhunt.Settings;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class WorldManager {

    private final Plugin plugin;
    private World manhuntWorld;
    private World manhuntNether;
    private World manhuntEnd;

    public WorldManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void loadWorlds() {
        manhuntWorld = deleteIfExists(Settings.worldName, World.Environment.NORMAL);
        manhuntNether = deleteIfExists(Settings.worldName + "_nether", World.Environment.NETHER);
        manhuntEnd = deleteIfExists(Settings.worldName + "_the_end", World.Environment.THE_END);
    }

    private World deleteIfExists(String s, World.Environment env) {
        World w = Bukkit.getWorld(s);
        if (w != null) {
            deleteWorld(w);
        }
        return createWorld(s, env);
    }

    private World createWorld(String s, World.Environment env) {
        World world = new WorldCreator(s).environment(env).createWorld();
        Bukkit.getWorlds().add(world);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setDifficulty(Difficulty.EASY);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(Settings.worldBorder * 2);
        world.setAutoSave(false);
        return world;
    }

    public void deleteAllFiles() {
        String s = plugin.getServer().getWorldContainer().getAbsolutePath();
        String name = Settings.worldName;
        deleteFile(new File(s + name));
        deleteFile(new File(s + name + "_nether"));
        deleteFile(new File(s + name + "_the_end"));
    }

    public void deleteAll() {
        unloadAndDelete(manhuntWorld).join();
        unloadAndDelete(manhuntNether).join();
        unloadAndDelete(manhuntEnd).join();

    }

    public CompletableFuture<Void> unloadAndDelete(final World w) {
        File f = w.getWorldFolder();
        return unloadWorld(w).thenRun(() -> deleteFile(f));
    }

    public CompletableFuture<Void> unloadWorld(final World w) {
        final UUID worldID = w.getUID();
        Bukkit.unloadWorld(w, true);
        return CompletableFuture.runAsync(() -> {
            while (Bukkit.getWorld(worldID) != null) {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void deleteWorld(World w) {
        String s = w.getWorldFolder().getAbsolutePath();
        Bukkit.unloadWorld(w, false);
        deleteFile(new File(s));
    }

    private void deleteFile(File file) {
        if (file.exists()) {
            try (Stream<Path> files = Files.walk(file.toPath())) {
                files.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .filter(f -> !f.equals(file))
                        .forEach(File::delete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public World getManhuntWorld() {
        return manhuntWorld;
    }

    public World getManhuntNether() {
        return manhuntNether;
    }

    public World getManhuntEnd() {
        return manhuntEnd;
    }
}
