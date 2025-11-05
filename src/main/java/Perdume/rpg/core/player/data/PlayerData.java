package Perdume.rpg.core.player.data;

import Perdume.rpg.core.player.stats.PlayerStats;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * [Null Pointer 해결] 플레이어의 모든 데이터를 담는 최상위 컨테이너 클래스입니다.
 * 생성자에서 PlayerStats 객체를 반드시 초기화합니다.
 */
public class PlayerData {
    private String islandId;
    private final Map<String, Long> bossClearTimestamps = new HashMap<>();
    private final Set<String> unlockedSpawns = new HashSet<>();
    private PlayerStats playerStats;

    /**
     * PlayerData 생성자
     * [핵심] 이 시점에서 PlayerStats 객체를 생성하여 Null Pointer Exception을 원천적으로 방지합니다.
     */
    public PlayerData() {
        this.playerStats = new PlayerStats();
    }

    public String getIslandId() {
        return islandId;
    }

    public void setIslandId(String islandId) {
        this.islandId = islandId;
    }

    public boolean hasIsland() {
        return islandId != null && !islandId.isEmpty();
    }

    public Map<String, Long> getBossClearTimestamps() {
        return bossClearTimestamps;
    }

    public long getLastClearTime(String bossId) {
        return bossClearTimestamps.getOrDefault(bossId, 0L);
    }

    public void setLastClearTime(String bossId, long timestamp) {
        bossClearTimestamps.put(bossId, timestamp);
    }

    public Set<String> getUnlockedSpawns() {
        return unlockedSpawns;
    }

    public void unlockSpawn(String spawnId) {
        unlockedSpawns.add(spawnId);
    }

    /**
     * [추가] 플레이어가 특정 스폰 지점을 잠금 해제했는지 확인합니다.
     * @param key 확인할 스폰 지점의 고유 키
     * @return 잠금 해제했다면 true
     */
    public boolean hasUnlockedSpawn(String key) {
        return unlockedSpawns.contains(key);
    }

    public PlayerStats getStats() {
        return playerStats;
    }

    public void setStats(PlayerStats playerStats) {
        this.playerStats = playerStats;
    }
}

