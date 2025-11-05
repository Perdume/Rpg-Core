package Perdume.rpg.gamemode.island;

import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;
import java.util.List;
import java.util.UUID;

/**
 * [리팩토링됨] 메모리에 로드된 '활성화된 섬'을 나타내는 '건설 현장' 클래스입니다.
 * 실제 월드, 보관함, 자동화 Task 등 런타임 객체들을 관리합니다.
 */
public class Island {

    private final IslandData islandData; // 이 섬의 원본 '설계도'
    private World world;
    private transient Inventory storage; // 'transient' 키워드로 저장 방지
    private transient BukkitTask autoMinerTask; // 'transient' 키워드로 저장 방지

    public Island(IslandData islandData) {
        this.islandData = islandData;
    }

    // --- 런타임 객체 Getter/Setter ---
    public World getWorld() { return world; }
    public void setWorld(World world) { this.world = world; }
    public Inventory getStorage() { return storage; }
    public void setStorage(Inventory storage) { this.storage = storage; }
    public BukkitTask getAutoMinerTask() { return autoMinerTask; }
    public void setAutoMinerTask(BukkitTask autoMinerTask) { this.autoMinerTask = autoMinerTask; }

    /**
     * 이 '건설 현장'의 기반이 되는 '설계도' 객체를 반환합니다.
     * @return IslandData 객체
     */
    public IslandData getData() {
        return islandData;
    }

    // --- 편의를 위해 '설계도'의 주요 정보에 바로 접근하는 메소드들 ---
    public String getId() { return islandData.getId(); }
    public UUID getOwner() { return islandData.getOwner(); }
    public List<UUID> getMembers() { return islandData.getMembers(); }
    public boolean isMember(UUID uuid) { return islandData.isMember(uuid); }
    public String getWorldName() { return islandData.getWorldName(); }
    public void broadcast(String message) { islandData.broadcast(message); }
}