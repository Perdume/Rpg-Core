package Perdume.rpg.gamemode.island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 섬의 모든 영구 데이터를 저장하는 '설계도' 클래스입니다. (Data Transfer Object)
 * 이 객체는 YML 파일에 직접 저장되고 로드됩니다.
 */
public class IslandData {

    private final String id;
    private final UUID owner;
    private final List<UUID> members;
    private Location spawnLocation;

    private String storageContents;

    // 업그레이드 티어 정보
    private int autoMinerTier;
    private int rareDropTier;
    private int multiDropTier;
    private int storageTier;

    private long lastUnloadTime;

    /**
     * 새로운 섬을 생성할 때 사용되는 생성자입니다.
     */
    public IslandData(Player owner) {
        // 기존 Island.java의 생성자 로직을 그대로 가져옵니다.
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.owner = owner.getUniqueId();
        this.members = new CopyOnWriteArrayList<>();
        this.members.add(owner.getUniqueId());

        // 업그레이드 기본값을 1티어로 설정합니다.
        this.autoMinerTier = 1;
        this.rareDropTier = 1;
        this.multiDropTier = 1;
        this.storageTier = 1;

        this.lastUnloadTime = 0;
    }

    /**
     * 파일(.yml)에서 데이터를 불러와 객체를 생성할 때 사용되는 생성자입니다.
     */
    public IslandData(String id, UUID owner, List<UUID> members, Location spawn, int autoMiner, int rareDrop, int multiDrop, int storage, long lastUnloadTime) {
        this.id = id.replace("Island--", "");
        this.owner = owner;
        this.members = new CopyOnWriteArrayList<>(members);
        this.spawnLocation = spawn;
        this.autoMinerTier = autoMiner;
        this.rareDropTier = rareDrop;
        this.multiDropTier = multiDrop;
        this.storageTier = storage;
        this.storageContents = "";
        this.lastUnloadTime = lastUnloadTime;
    }

    // --- 데이터 Getter 및 Setter ---
    public String getId() { return id; }
    public UUID getOwner() { return owner; }
    public List<UUID> getMembers() { return members; }
    public boolean isMember(UUID uuid) { return members.contains(uuid); }
    public void addMember(UUID uuid) { if (!members.contains(uuid)) members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public Location getSpawnLocation() { return spawnLocation; }
    public void setSpawnLocation(Location spawnLocation) { this.spawnLocation = spawnLocation; }
    public String getWorldName() {
        return "Island--" + this.id;
    }
    public String getTemplateFolderName() {
        return this.id;
    }
    public boolean hasIsland() { return this.id != null; }

    // --- 업그레이드 Getter 및 Setter ---
    public int getAutoMinerTier() { return autoMinerTier; }
    public void setAutoMinerTier(int tier) { this.autoMinerTier = tier; }
    public int getRareDropTier() { return rareDropTier; }
    public void setRareDropTier(int tier) { this.rareDropTier = tier; }
    public int getMultiDropTier() { return multiDropTier; }
    public void setMultiDropTier(int tier) { this.multiDropTier = tier; }
    public int getStorageTier() { return storageTier; }
    public void setStorageTier(int tier) { this.storageTier = tier; }
    public long getLastUnloadTime() { return lastUnloadTime; }
    public void setLastUnloadTime(long time) { this.lastUnloadTime = time; }

    /**
     * 섬의 모든 온라인 멤버에게 메시지를 전송합니다.
     */
    public void broadcast(String message) {
        members.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .forEach(p -> p.sendMessage("§e[섬] §f" + message));
    }

    /**
     * [핵심 추가] 로드된 보관함 데이터를 임시로 저장합니다.
     * @param contents Base64로 인코딩된 아이템 데이터 문자열
     */
    public void setStorageContents(String contents) {
        this.storageContents = contents;
    }

    /**
     * [핵심 추가] 임시 저장된 보관함 데이터를 반환합니다.
     * @return Base64로 인코딩된 아이템 데이터 문자열
     */
    public String getStorageContents() {
        return this.storageContents;
    }

}
