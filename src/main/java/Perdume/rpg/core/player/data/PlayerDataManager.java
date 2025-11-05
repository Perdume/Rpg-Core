package Perdume.rpg.core.player.data;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.player.stats.PlayerStats;
import Perdume.rpg.core.player.stats.StatType;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [스탯 저장 해결] 플레이어의 모든 데이터를 파일(.yml)로 저장하고 불러오는 클래스입니다.
 */
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
    }

    public PlayerData getPlayerData(OfflinePlayer player) {
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
        PlayerData data = new PlayerData(); // 이 시점에서 PlayerStats 객체는 기본값으로 생성됩니다.

        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            data.setIslandId(config.getString("island-id", null));

            ConfigurationSection clearSection = config.getConfigurationSection("boss-clears");
            if (clearSection != null) {
                clearSection.getKeys(false).forEach(bossId -> data.setLastClearTime(bossId, clearSection.getLong(bossId)));
            }
            data.getUnlockedSpawns().addAll(config.getStringList("unlocked-spawns"));

            // --- PlayerStats 데이터 로드 ---
            ConfigurationSection statsSection = config.getConfigurationSection("stats");
            if (statsSection != null) {
                PlayerStats stats = new PlayerStats();
                stats.setLevel(statsSection.getInt("level", 1));
                stats.setExperience(statsSection.getDouble("experience", 0.0));
                stats.setStatPoints(statsSection.getInt("stat-points", 0));

                ConfigurationSection baseStatsSection = statsSection.getConfigurationSection("base-stats");
                if (baseStatsSection != null) {
                    for (String key : baseStatsSection.getKeys(false)) {
                        try {
                            StatType type = StatType.valueOf(key.toUpperCase(Locale.ROOT));
                            stats.setStat(type, baseStatsSection.getInt(key));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                data.setStats(stats);
            }
        }
        return data;
    }

    /**
     * [핵심 수정] PlayerData 객체를 .yml 파일로 저장할 때, 'PlayerStats' 정보를 함께 저장합니다.
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
        config.set("unlocked-spawns", data.getUnlockedSpawns().stream().toList());

        // --- [핵심 추가] PlayerStats 데이터 저장 ---
        PlayerStats stats = data.getStats();
        if (stats != null) {
            config.set("stats.level", stats.getLevel());
            config.set("stats.experience", stats.getExperience());
            config.set("stats.stat-points", stats.getStatPoints());

            // 'base-stats' 섹션에 플레이어가 투자한 스탯 저장
            for (Map.Entry<StatType, Integer> entry : stats.getBaseStats().entrySet()) {
                // 키를 소문자로 저장하여 yml 파일의 가독성을 높입니다.
                config.set("stats.base-stats." + entry.getKey().name().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }

        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

