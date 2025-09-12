package Perdume.rpg.gamemode.field.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.*;

public class SpawnFinderUtil {

    public static List<Location> findSpawnableLocations(Location startLocation, int radius) {
        List<Location> spawnableLocations = new ArrayList<>();
        Queue<Location> queue = new LinkedList<>();
        Set<Location> visited = new HashSet<>();

        queue.add(startLocation);
        visited.add(startLocation.getBlock().getLocation());

        while (!queue.isEmpty()) {
            Location current = queue.poll();
            if (isSpawnable(current)) {
                spawnableLocations.add(current.getBlock().getLocation().add(0.5, 0, 0.5));
            }

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;

                    Location neighbor = current.clone().add(x, 0, z);
                    if (neighbor.distanceSquared(startLocation) > radius * radius) continue;

                    if (visited.add(neighbor.getBlock().getLocation())) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        return spawnableLocations;
    }

    private static boolean isSpawnable(Location location) {
        Block ground = location.getBlock().getRelative(BlockFace.DOWN);
        Block feet = location.getBlock();
        Block head = location.getBlock().getRelative(BlockFace.UP);
        return ground.getType().isSolid() && !feet.getType().isSolid() && !head.getType().isSolid();
    }
}