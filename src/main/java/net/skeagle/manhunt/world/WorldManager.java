package net.skeagle.manhunt.world;

import net.skeagle.manhunt.config.Settings;
import org.bukkit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class WorldManager {

    private final World manhuntWorld;
    private final World manhuntNether;
    private final World manhuntEnd;

    public WorldManager() {

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
        world.getWorldBorder().setSize(12000);
        return world;
    }

    public void deleteAll() {
        deleteWorld(manhuntWorld);
        deleteWorld(manhuntNether);
        deleteWorld(manhuntEnd);
    }

    private void deleteWorld(World w) {
        Bukkit.unloadWorld(w, true);
        String s = w.getWorldFolder().getAbsolutePath();
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
