package net.skeagle.manhunt.world;

import net.skeagle.manhunt.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;

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
        manhuntWorld = genWorldIfMissing(Settings.worldName, World.Environment.NORMAL);
        manhuntNether = genWorldIfMissing(Settings.worldName + "_nether", World.Environment.NETHER);
        manhuntEnd = genWorldIfMissing(Settings.worldName + "_the_end", World.Environment.THE_END);
    }

    private World genWorldIfMissing(String s, World.Environment env) {
        return Bukkit.getWorld(s) == null ? createWorld(s, env) : Bukkit.getWorld(s);
    }

    private World createWorld(String s, World.Environment env) {
        World world = new WorldCreator(s).environment(env).createWorld();
        Bukkit.getWorlds().add(world);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(6000);
        return world;
    }

    public void deleteAll() {
        deleteWorld(manhuntWorld);
        deleteWorld(manhuntNether);
        deleteWorld(manhuntEnd);
    }

    private void deleteWorld(World w) {
        if (w == null)
            return;
        Bukkit.unloadWorld(w, true);
        String s = w.getWorldFolder().getAbsolutePath();
        deleteFile(new File(s), true);
        deleteFile(new File(s + "_nether"), true);
        deleteFile(new File(s + "_the_end"), true);
    }

    private void deleteFile(File file, boolean contents) {
        if (file.exists()) {
            try (Stream<Path> files = Files.walk(file.toPath())) {
                files.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .filter(f -> contents && !f.equals(file))
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
