package Perdume.rpg.gamemode.island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Island {

    private final String id;
    private UUID owner;
    private final List<UUID> members = new ArrayList<>();
    private transient World world;
    private Location spawnLocation;

    public Island(Player owner) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.owner = owner.getUniqueId();
        this.members.add(owner.getUniqueId());
    }

    public Island(String id, UUID owner, List<UUID> members, Location spawnLocation) {
        this.id = id;
        this.owner = owner;
        this.members.addAll(members);
        this.spawnLocation = spawnLocation;
    }
    
    /**
     * [신규] 이 섬의 모든 온라인 멤버에게 메시지를 보냅니다.
     * @param message 보낼 메시지
     */
    public void broadcast(String message) {
        for (UUID memberUuid : members) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.isOnline()) {
                member.sendMessage("§b[섬] §f" + message);
            }
        }
    }

    // --- 나머지 모든 메소드는 이전과 동일 ---
    public String getId() { return id; }
    public UUID getOwner() { return owner; }
    public List<UUID> getMembers() { return Collections.unmodifiableList(members); }
    public World getWorld() { return world; }
    public Location getSpawnLocation() { return spawnLocation; }
    public String getWorldName() { return "Island--" + this.id; }
    public String getTemplateFolderName() { return this.id; }
    public void setWorld(World world) { this.world = world; }
    public void setSpawnLocation(Location location) { this.spawnLocation = location; }
    public boolean isMember(UUID uuid) { return members.contains(uuid); }
    public void addMember(UUID uuid) { if (!members.contains(uuid)) members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
}