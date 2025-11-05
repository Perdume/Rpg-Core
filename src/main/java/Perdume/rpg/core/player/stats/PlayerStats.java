package Perdume.rpg.core.player.stats;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 플레이어의 레벨, 경험치, 스탯 등 모든 성장 관련 데이터를 저장하고 관리하는 클래스입니다.
 * ConfigurationSerializable을 구현하여 YML 파일에 쉽게 저장하고 불러올 수 있습니다.
 */
public class PlayerStats implements ConfigurationSerializable {

    private int level;
    private double experience;
    private long playTime; // 초 단위
    private int statPoints;
    private Map<StatType, Integer> baseStats;

    // 새로운 플레이어를 위한 기본 생성자
    public PlayerStats() {
        this.level = 1;
        this.experience = 0.0;
        this.playTime = 0L;
        this.statPoints = 0;
        this.baseStats = new HashMap<>();
        // 모든 스탯을 기본값 5로 초기화합니다.
        for (StatType type : StatType.values()) {
            baseStats.put(type, 5);
        }
    }

    // --- Getter와 Setter ---
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public double getExperience() { return experience; }
    public void setExperience(double experience) { this.experience = experience; }
    public long getPlayTime() { return playTime; }
    public void addPlayTime(long seconds) { this.playTime += seconds; }
    public int getStatPoints() { return statPoints; }
    public void setStatPoints(int statPoints) { this.statPoints = statPoints; }
    public Map<StatType, Integer> getBaseStats() { return baseStats; }
    public int getStat(StatType type) { return baseStats.getOrDefault(type, 0); }
    public void setStat(StatType type, int value) { baseStats.put(type, value); }


    // --- YML 파일 저장을 위한 직렬화/역직렬화 ---

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("level", level);
        data.put("experience", experience);
        data.put("playTime", playTime);
        data.put("statPoints", statPoints);

        Map<String, Integer> statsMap = new HashMap<>();
        for (Map.Entry<StatType, Integer> entry : baseStats.entrySet()) {
            statsMap.put(entry.getKey().name(), entry.getValue());
        }
        data.put("baseStats", statsMap);
        return data;
    }

    public static PlayerStats deserialize(Map<String, Object> data) {
        PlayerStats stats = new PlayerStats();
        stats.level = (int) data.getOrDefault("level", 1);
        stats.experience = (double) data.getOrDefault("experience", 0.0);
        stats.playTime = ((Number) data.getOrDefault("playTime", 0L)).longValue();
        stats.statPoints = (int) data.getOrDefault("statPoints", 0);

        if (data.containsKey("baseStats")) {
            Map<String, Integer> statsMap = (Map<String, Integer>) data.get("baseStats");
            for (Map.Entry<String, Integer> entry : statsMap.entrySet()) {
                try {
                    stats.baseStats.put(StatType.valueOf(entry.getKey()), entry.getValue());
                } catch (IllegalArgumentException ignored) {
                    // YML 파일에 잘못된 스탯 이름이 있어도 무시하고 넘어갑니다.
                }
            }
        }
        return stats;
    }
}
