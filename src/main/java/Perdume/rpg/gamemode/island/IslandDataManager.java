package Perdume.rpg.gamemode.island;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.util.InventorySerializer;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * [시간 저장/로드 추가] 모든 섬의 'IslandData'를 파일(.yml)로 저장하고 불러옵니다.
 * 마지막 언로드 시간(lastUnloadTime)을 함께 처리합니다.
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

    /**
     * IslandData 객체와 섬 보관함(Inventory)의 내용을 .yml 파일로 저장합니다.
     * @param islandData 저장할 섬의 데이터 객체
     * @param storage 저장할 섬의 보관함 인벤토리 (null 가능)
     */
    public void saveIslandData(IslandData islandData, Inventory storage) {
        if (islandData == null) return;
        File islandFile = new File(dataFolder, islandData.getId() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("owner", islandData.getOwner().toString());
        config.set("members", islandData.getMembers().stream().map(UUID::toString).collect(Collectors.toList()));

        if (islandData.getSpawnLocation() != null) {
            Location loc = islandData.getSpawnLocation();
            // 월드 이름은 저장하지 않거나, 참고용으로만 저장합니다. 로드 시에는 실제 월드 객체를 주입해야 합니다.
            config.set("spawn-location.world-name", loc.getWorld().getName());
            config.set("spawn-location.x", loc.getX());
            config.set("spawn-location.y", loc.getY());
            config.set("spawn-location.z", loc.getZ());
            config.set("spawn-location.yaw", loc.getYaw());
            config.set("spawn-location.pitch", loc.getPitch());
        }

        config.set("upgrades.auto-miner", islandData.getAutoMinerTier());
        config.set("upgrades.rare-drop", islandData.getRareDropTier());
        config.set("upgrades.multi-drop", islandData.getMultiDropTier());
        config.set("upgrades.storage", islandData.getStorageTier());

        // [핵심] 마지막 언로드 시간 저장
        config.set("last-unload-time", islandData.getLastUnloadTime());

        if (storage != null) {
            config.set("storage-contents", InventorySerializer.inventoryToBase64(storage));
        }

        try {
            config.save(islandFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 파일(.yml)에서 섬 데이터를 불러와 IslandData 객체로 반환합니다.
     * @param islandId 불러올 섬의 ID ('Island--' 접두사가 없는 순수 ID)
     * @return IslandData 객체, 파일이 없으면 null
     */
    public IslandData loadIslandData(String islandId) {
        File islandFile = new File(dataFolder, islandId + ".yml");
        if (!islandFile.exists()) return null;

        FileConfiguration config = YamlConfiguration.loadConfiguration(islandFile);

        try {
            String id = islandFile.getName().replace(".yml", "");
            UUID owner = UUID.fromString(config.getString("owner"));
            List<UUID> members = config.getStringList("members").stream().map(UUID::fromString).collect(Collectors.toList());
            Location spawnLocation = null;

            if (config.isConfigurationSection("spawn-location")) {
                double x = config.getDouble("spawn-location.x");
                double y = config.getDouble("spawn-location.y");
                double z = config.getDouble("spawn-location.z");
                float yaw = (float) config.getDouble("spawn-location.yaw");
                float pitch = (float) config.getDouble("spawn-location.pitch");
                spawnLocation = new Location(null, x, y, z, yaw, pitch); // 월드는 나중에 주입
            }

            int autoMiner = config.getInt("upgrades.auto-miner", 1);
            int rareDrop = config.getInt("upgrades.rare-drop", 1);
            int multiDrop = config.getInt("upgrades.multi-drop", 1);
            int storage = config.getInt("upgrades.storage", 1);

            // [핵심] 마지막 언로드 시간 불러오기 (없으면 0)
            long lastUnloadTime = config.getLong("last-unload-time", 0);

            IslandData islandData = new IslandData(id, owner, members, spawnLocation, autoMiner, rareDrop, multiDrop, storage, lastUnloadTime);
            islandData.setStorageContents(config.getString("storage-contents", ""));

            return islandData;

        } catch (Exception e) {
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

