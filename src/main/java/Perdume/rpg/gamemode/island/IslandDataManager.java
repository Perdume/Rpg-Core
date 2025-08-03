package Perdume.rpg.gamemode.island;

import Perdume.rpg.Rpg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 모든 섬의 영구 데이터를 파일(.yml)로 저장하고 불러오는 클래스입니다.
 */
public class IslandDataManager {

    private final Rpg plugin;
    private final File dataFolder;

    public IslandDataManager(Rpg plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "island_data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void saveIsland(Island island) {
        if (island == null) return;
        File islandFile = new File(dataFolder, island.getId() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("owner", island.getOwner().toString());
        config.set("members", island.getMembers().stream().map(UUID::toString).collect(Collectors.toList()));

        if (island.getSpawnLocation() != null) {
            config.set("spawn-location", island.getSpawnLocation());
        }

        try {
            config.save(islandFile);
        } catch (IOException e) {
            Rpg.log.severe("섬 데이터 저장 실패: " + island.getId());
            e.printStackTrace();
        }
    }

    /**
     * [핵심 수정] 파일에서 섬 ID를 기반으로 Island 객체를 불러옵니다.
     * @param islandId 불러올 섬의 고유 ID
     * @return 복원된 Island 객체, 파일이 없으면 null
     */
    public Island loadIsland(String islandId) {
        File islandFile = new File(dataFolder, islandId + ".yml");
        if (!islandFile.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(islandFile);

        try {
            UUID owner = UUID.fromString(config.getString("owner"));
            List<UUID> members = config.getStringList("members").stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());

            // 1. 파일에서 Location 객체를 불러옵니다.
            Location spawnLocation = config.getLocation("spawn-location");

            // 2. [핵심] 만약 불러온 값이 null이라면 (파일에 정보가 없다면),
            //    임시로 메인 월드를 기준으로 한 기본 위치를 생성합니다.
            if (spawnLocation == null) {
                // 이 월드 정보는 나중에 SkyblockManager가 실제 섬 월드로 교체해 줄 것입니다.
                spawnLocation = new Location(Bukkit.getWorlds().get(0), 0.5, 65, 0.5);
            }

            return new Island(islandId, owner, members, spawnLocation);

        } catch (Exception e) {
            Rpg.log.severe("섬 데이터 불러오기 실패: " + islandId);
            e.printStackTrace();
            return null;
        }
    }

    public void deleteIslandData(String islandId) {
        File islandFile = new File(dataFolder, islandId + ".yml");
        if (islandFile.exists()) {
            islandFile.delete();
        }
    }
}