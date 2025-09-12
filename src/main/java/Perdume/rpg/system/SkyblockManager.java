package Perdume.rpg.system; // 사용하시는 패키지 경로

import Perdume.rpg.Rpg;
import Perdume.rpg.core.player.data.PlayerData;
import Perdume.rpg.core.player.data.PlayerDataManager;
import Perdume.rpg.gamemode.island.Island;
import Perdume.rpg.gamemode.island.IslandDataManager;
import Perdume.rpg.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SkyblockManager {

    private final Rpg plugin;
    // 현재 메모리에 로드된 섬들의 목록 (ID를 키로 사용)
    private final Map<String, Island> activeIslands = new HashMap<>();
    // 보낸 초대장을 관리 <초대받은사람 UUID, 초대한 섬 ID>
    private final Map<UUID, String> invites = new HashMap<>();
    private final PlayerDataManager playerDataManager;
    private final IslandDataManager islandDataManager;

    public SkyblockManager(Rpg plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        this.islandDataManager = new IslandDataManager(plugin);
    }

    /**
     * 플레이어의 섬 객체를 가져옵니다. 메모리에 없으면 파일에서 로드합니다.
     * @param player 대상 플레이어
     * @return 소속된 Island 객체, 없으면 null
     */
    public Island getIsland(Player player) {
        String islandId = playerDataManager.getPlayerData(player).getIslandId();
        if (islandId == null) return null;
        return activeIslands.computeIfAbsent(islandId, islandDataManager::loadIsland);
    }

    /**
     * 섬 ID로 섬 객체를 가져옵니다. 메모리에 없으면 파일에서 로드합니다.
     */
    public Island getIslandById(String id) {
        if (id == null) return null;
        return activeIslands.computeIfAbsent(id, islandDataManager::loadIsland);
    }

    /**
     * 서버에 존재하는 모든 섬의 목록을 반환합니다 (관리자용).
     */
    public List<Island> getAllIslands() {
        List<Island> allIslands = new ArrayList<>();
        File dataFolder = new File(plugin.getDataFolder(), "island_data");
        File[] islandFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (islandFiles != null) {
            for (File file : islandFiles) {
                String islandId = file.getName().replace(".yml", "");
                allIslands.add(getIslandById(islandId));
            }
        }
        return allIslands;
    }

    /**
     * [핵심 수정] 플레이어를 자신의 섬으로 이동시킵니다.
     * 모든 판단은 '플레이어 데이터'를 기준으로 시작합니다.
     */
    public void enterMyIsland(Player player) {
        String islandId = playerDataManager.getPlayerData(player).getIslandId();
        if (islandId == null || islandId.isEmpty()) {
            player.sendMessage("§c아직 당신의 섬이 없습니다. /섬 생성 명령어로 만들어주세요.");
            return;
        }

        Rpg.log.info("[디버그] " + player.getName() + "님이 섬(" + islandId + ") 입장을 시도합니다.");

        Island island = activeIslands.get(islandId);
        if (island != null && island.getWorld() != null) {
            Rpg.log.info("[디버그] 섬(" + islandId + ")은(는) 이미 로드되어 있어, 즉시 텔레포트합니다.");
            player.teleport(island.getSpawnLocation() != null ? island.getSpawnLocation() : island.getWorld().getSpawnLocation());
        } else {
            player.sendMessage("§e당신의 섬을 불러오는 중입니다...");
            Rpg.log.info("[디버그] 섬(" + islandId + ")이(가) 언로드 상태이므로, 로딩 절차를 시작합니다.");
            loadAndEnterIsland(islandId, player);
        }
    }

    /**
     * [핵심 수정] 파일에서 섬을 로드하고 플레이어를 입장시키는 절차를 통합합니다.
     */
    private void loadAndEnterIsland(String islandId, Player player) {
        Island island = islandDataManager.loadIsland(islandId);
        if (island == null) {
            player.sendMessage("§c섬 데이터를 불러오는 데 실패했습니다.");
            return;
        }

        activeIslands.put(islandId, island);

        WorldManager.copyAndLoadWorld(island.getWorldName(), island.getTemplateFolderName(), "island", (world) -> {
            if (world != null) {
                island.setWorld(world);

                // --- [핵심 수정] ---
                // 1. 섬의 스폰 위치(주소)를 가져옵니다.
                Location spawnLocation = island.getSpawnLocation();

                // [핵심] 만약 저장된 스폰 위치가 있다면, 그 위치의 월드 정보를
                // 우리가 방금 새로 로드한 'world'로 갱신합니다.
                if (spawnLocation != null) {
                    spawnLocation.setWorld(world);
                } else {
                    // 저장된 스폰 위치가 없다면, 월드의 기본 스폰을 사용합니다.
                    spawnLocation = world.getSpawnLocation();
                }

                // 3. 이제 '주소'와 '집'이 일치하므로, 안전하게 텔레포트합니다.
                player.teleport(spawnLocation);
            } else {
                player.sendMessage("§c섬 월드를 불러오는 데 실패했습니다.");
                activeIslands.remove(islandId);
            }
        });
    }
    /**
     * 새로운 섬을 생성합니다.
     */
    public void createIsland(Player player) {
        if (playerDataManager.getPlayerData(player).hasIsland()) {
            player.sendMessage("§c이미 섬을 소유하고 있습니다.");
            return;
        }

        Island newIsland = new Island(player);
        activeIslands.put(newIsland.getId(), newIsland);
        playerDataManager.getPlayerData(player).setIslandId(newIsland.getId());
        islandDataManager.saveIsland(newIsland);

        player.sendMessage("§a당신만의 새로운 섬을 생성하는 중입니다...");
        WorldManager.copyAndLoadWorld(newIsland.getWorldName(), "island_template", "island", (world) -> {
            if (world != null) {
                newIsland.setWorld(world);
                newIsland.setSpawnLocation(world.getSpawnLocation()); // 초기 스폰 위치 설정
                islandDataManager.saveIsland(newIsland); // 스폰 위치 저장
                player.teleport(world.getSpawnLocation());
                player.sendMessage("§a하늘에 떠 있는 당신의 섬이 생성되었습니다!");
            } else {
                player.sendMessage("§c섬 생성에 실패했습니다.");
                activeIslands.remove(newIsland.getId());
                playerDataManager.getPlayerData(player).setIslandId(null);
            }
        });
    }



    public void inviteToIsland(Player inviter, Player target) {
        Island island = getIsland(inviter);
        if (island == null || !island.getOwner().equals(inviter.getUniqueId())) {
            inviter.sendMessage("§c섬의 주인이 아니면 초대할 수 없습니다.");
            return;
        }
        if (target == null || !target.isOnline()) { inviter.sendMessage("§c초대할 플레이어를 찾을 수 없습니다."); return; }
        if (playerDataManager.getPlayerData(target).hasIsland()) {
            inviter.sendMessage("§c" + target.getName() + "님은 이미 섬이 있습니다.");
            return;
        }

        invites.put(target.getUniqueId(), island.getId());
        inviter.sendMessage("§a" + target.getName() + "님을 섬에 초대했습니다.");
        target.sendMessage("§a" + inviter.getName() + "님의 섬(" + island.getId() + ")에 초대되었습니다.");
        target.sendMessage("§e/섬 수락 " + inviter.getName() + " §f명령어로 수락하세요.");
    }

    /**
     * 섬에서 특정 멤버를 추방합니다. (섬주인 전용)
     */
    public void kickFromIsland(Player owner, Player target) {
        Island island = getIsland(owner);
        if (island == null || !island.getOwner().equals(owner.getUniqueId())) {
            owner.sendMessage("§c섬의 주인이 아니면 추방할 수 없습니다.");
            return;
        }
        if (target == null) {
            owner.sendMessage("§c추방할 플레이어를 찾을 수 없습니다.");
            return;
        }
        if (owner.equals(target)) {
            owner.sendMessage("§c자기 자신을 추방할 수 없습니다.");
            return;
        }
        if (!island.isMember(target.getUniqueId())) {
            owner.sendMessage("§c" + target.getName() + "님은 당신의 섬 멤버가 아닙니다.");
            return;
        }

        island.removeMember(target.getUniqueId());
        playerDataManager.getPlayerData(target).setIslandId(null);
        islandDataManager.saveIsland(island);

        owner.sendMessage("§a" + target.getName() + "님을 섬에서 추방했습니다.");
        if (target.isOnline()) {
            target.sendMessage("§c" + owner.getName() + "님의 섬에서 추방당했습니다.");
        }
    }

    /**
     * [핵심 수정] 섬 월드의 변경사항을 영구적으로 저장하고 메모리에서 언로드합니다.
     * @param islandId 언로드할 섬의 고유 ID
     */
    public void unloadIsland(String islandId) {
        // 1. 현재 메모리에 로드된 섬 목록에서 해당 Island 객체를 가져옵니다.
        Island island = activeIslands.get(islandId);
        if (island == null || island.getWorld() == null) {
            return;
        }

        World worldToSave = island.getWorld();
        Rpg.log.info("섬(" + worldToSave.getName() + ") 저장을 시작합니다...");

        // 2. WorldManager를 통해 현재 월드의 상태를 템플릿 폴더에 덮어씌워 저장합니다.
        // 이 작업이 완료되면, 임시 월드(Island--<id>)는 자동으로 삭제됩니다.
        WorldManager.saveAndOverwriteTemplate(worldToSave, island.getTemplateFolderName(), "island", (success) -> {
            if (success) {
                Rpg.log.info("섬(" + worldToSave.getName() + ") 저장이 완료되었습니다.");
            } else {
                Rpg.log.severe("섬(" + worldToSave.getName() + ") 저장에 실패했습니다!");
            }
        });

        // 3. 섬 데이터를 파일(.yml)에 한 번 더 저장하여 최신 상태를 보장합니다.
        islandDataManager.saveIsland(island);

        // 4. 메모리에서 Island 객체와 월드 객체의 참조를 제거합니다.
        island.setWorld(null);
        activeIslands.remove(islandId);
    }


    /**
     * 현재 소속된 섬에서 나갑니다.
     */
    public void acceptInvite(Player player, Player inviter) {
        String islandId = invites.get(player.getUniqueId());
        if (islandId == null) { player.sendMessage("§c받은 초대가 없습니다."); return; }

        Island island = getIslandById(islandId);
        if (island == null || !island.getOwner().equals(inviter.getUniqueId())) {
            player.sendMessage("§c" + inviter.getName() + "님에게 받은 섬 초대가 없습니다.");
            return;
        }

        island.addMember(player.getUniqueId());
        playerDataManager.getPlayerData(player).setIslandId(island.getId());
        invites.remove(player.getUniqueId());

        islandDataManager.saveIsland(island);
        // [핵심] 섬 멤버들에게 합류 메시지를 보냅니다.
        island.broadcast(player.getName() + "님이 섬에 합류했습니다.");
    }

    public void leaveIsland(Player player) {
        Island island = getIsland(player);
        if (island == null) {
            player.sendMessage("§c소속된 섬이 없습니다.");
            return;
        }
        if (island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§c섬의 주인은 섬을 나갈 수 없습니다. 섬을 삭제하거나 소유권을 넘겨주세요.");
            return;
        }

        // [핵심] 섬 멤버들에게 탈퇴 메시지를 보냅니다.
        island.broadcast(player.getName() + "님이 섬에서 나갔습니다.");

        island.removeMember(player.getUniqueId());
        playerDataManager.getPlayerData(player).setIslandId(null);
        islandDataManager.saveIsland(island);

        player.sendMessage("§e섬에서 탈퇴했습니다.");
    }

    /**
     * 현재 섬의 정보를 보여줍니다.
     */
    public void showIslandInfo(Player player) {
        Island island = getIsland(player);
        if (island == null) {
            player.sendMessage("§c소속된 섬이 없습니다.");
            return;
        }

        player.sendMessage("§6--- 섬 정보 (ID: " + island.getId() + ") ---");
        String ownerName = Bukkit.getOfflinePlayer(island.getOwner()).getName();
        player.sendMessage("§e소유자: §f" + ownerName);
        String members = island.getMembers().stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                .collect(Collectors.joining(", "));
        player.sendMessage("§e멤버: §f" + members);
    }

    public void setHome(Player player) {
        Island island = getIsland(player);
        if (island == null || !island.isMember(player.getUniqueId())) {
            player.sendMessage("§c섬에 소속되어 있어야 홈을 설정할 수 있습니다.");
            return;
        }
        if (island.getWorld() == null || !player.getWorld().equals(island.getWorld())) {
            player.sendMessage("§c섬 안에서만 홈을 설정할 수 있습니다.");
            return;
        }

        island.setSpawnLocation(player.getLocation());
        islandDataManager.saveIsland(island);
        player.sendMessage("§a현재 위치를 섬의 새로운 스폰 지점으로 설정했습니다.");
    }

    // --- 관리자용 메소드 ---
    public void deleteIsland(OfflinePlayer target, CommandSender remover) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (!data.hasIsland()) {
            remover.sendMessage("§c" + target.getName() + "님은 섬을 소유하고 있지 않습니다.");
            return;
        }

        String islandId = data.getIslandId();
        Island island = getIslandById(islandId);

        if (island != null) {
            // 모든 멤버의 데이터에서 섬 ID 제거
            for (UUID memberId : island.getMembers()) {
                plugin.getPlayerDataManager().getPlayerData(Bukkit.getOfflinePlayer(memberId)).setIslandId(null);
            }

            // 월드가 로드되어 있다면 언로드 후 삭제
            if (island.getWorld() != null) {
                WorldManager.unloadAnddeleteWorld(island.getWorldName());
            }
            // 월드 템플릿(영구 데이터) 삭제
            WorldManager.deleteWorldFolder(new File(plugin.getDataFolder(), "worlds/island/" + island.getTemplateFolderName()));
            // 섬 데이터 파일 삭제
            islandDataManager.deleteIslandData(islandId);
            activeIslands.remove(islandId);

            remover.sendMessage("§a" + target.getName() + "님의 섬(" + islandId + ")을 성공적으로 삭제했습니다.");
        }
    }

    /**
     * [핵심] 관리자가 다른 플레이어의 섬으로 텔레포트합니다.
     * @param admin 텔레포트할 관리자
     * @param target 섬 주인의 정보
     */
    public void teleportToIsland(Player admin, OfflinePlayer target) {
        String islandId = playerDataManager.getPlayerData(target).getIslandId();
        if (islandId == null) {
            admin.sendMessage("§c" + target.getName() + "님은 섬을 소유하고 있지 않습니다.");
            return;
        }

        Island island = activeIslands.get(islandId);
        if (island != null && island.getWorld() != null) {
            admin.teleport(island.getSpawnLocation() != null ? island.getSpawnLocation() : island.getWorld().getSpawnLocation());
            admin.sendMessage("§a" + target.getName() + "님의 섬으로 이동했습니다.");
        } else {
            admin.sendMessage("§e" + target.getName() + "님의 섬을 불러오는 중입니다...");
            loadAndEnterIsland(islandId, admin);
        }
    }

    public Map<String, Island> getActiveIslands() { return activeIslands; }
}