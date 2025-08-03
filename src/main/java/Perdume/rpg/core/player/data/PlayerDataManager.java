package Perdume.rpg.core.player.data;

import Perdume.rpg.Rpg;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final Rpg plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final File dataFolder;

    public PlayerDataManager(Rpg plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // --- [핵심] 1. 플러그인 시작 시, 저장된 데이터 파일의 개수를 미리 확인하고 로그를 남깁니다. ---
        File[] playerFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles != null && playerFiles.length > 0) {
            Rpg.log.info(playerFiles.length + "개의 플레이어 데이터를 감지했습니다. 플레이어 접속 시 데이터를 불러옵니다.");
        } else {
            Rpg.log.info("저장된 플레이어 데이터가 없습니다.");
        }
    }

    public PlayerData getPlayerData(OfflinePlayer player) {
        // computeIfAbsent: 맵에 UUID가 없으면, loadPlayerData를 실행하여 결과를 맵에 넣고 반환
        return playerDataMap.computeIfAbsent(player.getUniqueId(), this::loadPlayerData);
    }

    public void loadPlayerDataOnJoin(Player player) {
        getPlayerData(player);
    }

    public void savePlayerDataOnQuit(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            savePlayerData(player.getUniqueId(), data);
            playerDataMap.remove(player.getUniqueId());
        }
    }

    private PlayerData loadPlayerData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid + ".yml");
        PlayerData data = new PlayerData();
        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            data.setIslandId(config.getString("island-id", null));

            ConfigurationSection clearSection = config.getConfigurationSection("boss-clears");
            if (clearSection != null) {
                clearSection.getKeys(false).forEach(bossId -> {
                    data.setLastClearTime(bossId, clearSection.getLong(bossId));
                });
            }

            // [핵심] 스폰 해금 목록을 불러옵니다.
            data.getUnlockedSpawns().addAll(config.getStringList("unlocked-spawns"));
        }
        return data;
    }

    /**
     * [핵심 수정] PlayerData 객체를 .yml 파일로 저장할 때, '마을 해금 목록'을 함께 저장합니다.
     */
    private void savePlayerData(UUID uuid, PlayerData data) {
        File playerFile = new File(dataFolder, uuid + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("island-id", data.getIslandId());

        if (data.getBossClearTimestamps() != null && !data.getBossClearTimestamps().isEmpty()) {
            for (Map.Entry<String, Long> entry : data.getBossClearTimestamps().entrySet()) {
                config.set("boss-clears." + entry.getKey(), entry.getValue());
            }
        }

        // [핵심] 'unlocked-spawns'라는 이름으로 해금된 마을 목록을 파일에 저장합니다.
        config.set("unlocked-spawns", data.getUnlockedSpawns());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean canClearBoss(Player player, String bossId) {
        long lastClear = getPlayerData(player).getLastClearTime(bossId);
        if (lastClear == 0) return true;
        long lastClearDay = (lastClear + 32400000) / 86400000;
        long today = (System.currentTimeMillis() + 32400000) / 86400000;
        return lastClearDay != today;
    }

    public void recordBossClear(Player player, String bossId) {
        getPlayerData(player).setLastClearTime(bossId, System.currentTimeMillis());
    }
}