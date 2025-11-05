package Perdume.rpg.system;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.util.InventorySerializer;
import Perdume.rpg.gamemode.island.Island;
import Perdume.rpg.gamemode.island.IslandData;
import Perdume.rpg.gamemode.island.IslandDataManager;
import Perdume.rpg.gamemode.island.task.AutoMinerTask;
import Perdume.rpg.gamemode.island.upgrade.IslandUpgrade;
import Perdume.rpg.gamemode.island.util.IslandProductionUtil;
import Perdume.rpg.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * [최종 수정본] 모든 섬의 생성, 로드, 언로드 및 조회를 총괄하는 중앙 관리자 클래스입니다.
 * IslandData(설계도)와 Island(건설 현장)의 역할 분리 원칙을 따릅니다.
 */
public class SkyblockManager {

    private final Rpg plugin;
    private final IslandDataManager dataManager;
    private final Map<String, Island> loadedIslands = new HashMap<>();
    private final Map<UUID, String> invites = new HashMap<>();
    private final Set<String> islandsBeingLoaded = new HashSet<>();

    public SkyblockManager(Rpg plugin) {
        this.plugin = plugin;
        this.dataManager = new IslandDataManager(plugin);
    }

    // --- 섬 생성, 로드, 언로드 ---

    public void createIsland(Player player) {
        if (plugin.getPlayerDataManager().getPlayerData(player).hasIsland()) {
            player.sendMessage("§c이미 섬을 소유하고 있습니다.");
            return;
        }

        IslandData newIslandData = new IslandData(player);
        plugin.getPlayerDataManager().getPlayerData(player).setIslandId(newIslandData.getId());
        dataManager.saveIslandData(newIslandData, null);

        player.sendMessage("§a당신만의 새로운 섬을 생성하는 중입니다...");

        // IslandData에서 'Island--'가 포함된 정확한 월드 이름을 가져와 사용합니다.
        WorldManager.copyAndLoadWorld(newIslandData.getWorldName(), "island_template", "island", (world) -> {
            if (world != null) {
                // 최초 생성 시, 생성된 월드를 이 섬 전용 템플릿으로 저장합니다.
                WorldManager.saveAndOverwriteTemplate(world, newIslandData.getTemplateFolderName(), "island", success -> {
                    if (success) {
                        newIslandData.setSpawnLocation(world.getSpawnLocation());
                        dataManager.saveIslandData(newIslandData, null);
                        // 저장이 완료된 후, 섬으로 텔레포트합니다.
                        teleportToIsland(player);
                    } else {
                        player.sendMessage("§c섬 생성 후 초기 저장에 실패했습니다.");
                        plugin.getPlayerDataManager().getPlayerData(player).setIslandId(null);
                        // 실패 시 생성된 월드 폴더와 데이터 파일을 삭제하는 로직 추가 가능
                    }
                });
            } else {
                player.sendMessage("§c섬 월드 생성에 실패했습니다.");
                plugin.getPlayerDataManager().getPlayerData(player).setIslandId(null);
            }
        });
    }

    public void teleportToIsland(Player player) {
        getOrLoadIsland(player, island -> {
            if (island != null) {
                player.teleport(getSafeSpawnLocation(island));
                player.sendMessage("§a섬에 오신 것을 환영합니다!");
            }
        });
    }

    private void loadIsland(String islandId, Consumer<Island> callback) {
        islandsBeingLoaded.add(islandId);
        IslandData islandData = dataManager.loadIslandData(islandId);

        if (islandData == null) {
            islandsBeingLoaded.remove(islandId);
            callback.accept(null);
            return;
        }

        // [디버그 로그 추가] 로드된 데이터로 어떤 월드 이름을 생성하는지 출력합니다.
        Rpg.log.info("[디버그] 섬 데이터 로드 완료. 월드 생성 시도 이름: " + islandData.getWorldName());

        Island island = new Island(islandData);

        WorldManager.copyAndLoadWorld(island.getWorldName(), island.getData().getTemplateFolderName(), "island", (world) -> {
            if (world != null) {
                island.setWorld(world);
                setupIslandSystems(island);
                loadedIslands.put(islandId, island);
                islandsBeingLoaded.remove(islandId);
                callback.accept(island);
            } else {
                islandsBeingLoaded.remove(islandId);
                callback.accept(null);
            }
        });
    }

    public void unloadIsland(String islandId) {
        Island island = loadedIslands.get(islandId);
        if (island == null) return;

        // [핵심] 자동 생산 Task를 안전하게 중지합니다.
        if (island.getAutoMinerTask() != null && !island.getAutoMinerTask().isCancelled()) {
            island.getAutoMinerTask().cancel();
        }

        // 보관함 아이템을 파일에 저장합니다.
        dataManager.saveIslandData(island.getData(), island.getStorage());

        World worldToSave = island.getWorld();
        if (worldToSave != null) {
            WorldManager.saveAndOverwriteTemplate(worldToSave, island.getData().getTemplateFolderName(), "island", (success) -> {
                if (success) Rpg.log.info(island.getWorldName() + " 섬이 안전하게 저장 및 언로드되었습니다.");
                else Rpg.log.severe(island.getWorldName() + " 섬 저장에 실패했습니다!");
            });
        }

        loadedIslands.remove(islandId);
    }

    private void calculateOfflineProduction(IslandData data, Inventory storage) {
        long lastUnloadTime = data.getLastUnloadTime();
        if (lastUnloadTime == 0) return; // 이전에 저장된 기록이 없으면 종료

        long offlineMillis = System.currentTimeMillis() - lastUnloadTime;
        long offlineSeconds = offlineMillis / 1000;
        if (offlineSeconds <= 0) return;

        double productionPeriod = IslandUpgrade.AUTO_MINER.getValue(data.getAutoMinerTier());
        int productionCycles = (int) (offlineSeconds / productionPeriod);

        if (productionCycles <= 0) return;

        int itemsProduced = 0;
        int maxStorageSize = storage.getSize() * 64; // 대략적인 최대 보관량

        for (int i = 0; i < productionCycles; i++) {
            // 창고가 꽉 찼으면 더 이상 아이템을 추가하지 않습니다.
            if (storage.firstEmpty() == -1) {
                break;
            }

            // AutoMinerTask와 동일한 로직으로 생산할 아이템을 결정합니다.
            int dropAmount = 1;
            double multiDropChance = IslandUpgrade.MULTI_DROP.getValue(data.getMultiDropTier());
            double random = ThreadLocalRandom.current().nextDouble(100.0);

            if (data.getMultiDropTier() == 5) {
                if (random < 5.0) dropAmount = 3;
                else if (random < 35.0) dropAmount = 2;
            } else {
                if (random < multiDropChance) dropAmount = 2;
            }

            ItemStack itemToProduce;
            double itemChance = ThreadLocalRandom.current().nextDouble(100.0);

            itemToProduce = IslandProductionUtil.generateRandomItem(data);

            itemToProduce.setAmount(dropAmount);
            storage.addItem(itemToProduce);
            itemsProduced += dropAmount;
        }

        if (itemsProduced > 0) {
            // 오프라인이었던 멤버에게만 접속 시 알림을 보낼 수 있도록 로직 추가 (추후 구현)
            Rpg.log.info(data.getWorldName() + " 섬에 오프라인 생산량 " + itemsProduced + "개를 추가했습니다.");
        }
    }

    private void setupIslandSystems(Island island) {
        IslandData data = island.getData();
        int storageTier = data.getStorageTier();
        int storageRows = (int) IslandUpgrade.STORAGE.getValue(storageTier);
        Inventory storage = Bukkit.createInventory(null, storageRows * 9, "섬 보관함");

        try {
            InventorySerializer.inventoryFromBase64(data.getStorageContents(), storage);
        } catch (IOException e) {
            e.printStackTrace();
            Rpg.log.severe(island.getWorldName() + " 섬의 보관함 아이템을 불러오는 데 실패했습니다.");
        }

        // [핵심] 오프라인 생산량을 계산하여 창고에 채워넣습니다.
        calculateOfflineProduction(data, storage);

        island.setStorage(storage);

        long period = (long) IslandUpgrade.AUTO_MINER.getValue(data.getAutoMinerTier()) * 20;
        island.setAutoMinerTask(new AutoMinerTask(island).runTaskTimer(plugin, period, period));
    }


    public void unloadIslandSync(String islandId) {
        Island island = loadedIslands.get(islandId);
        if (island == null) return;

        if (island.getAutoMinerTask() != null && !island.getAutoMinerTask().isCancelled()) {
            island.getAutoMinerTask().cancel();
        }

        island.getData().setLastUnloadTime(System.currentTimeMillis());
        dataManager.saveIslandData(island.getData(), island.getStorage());

        WorldManager.saveAndOverwriteTemplateSync(island.getWorld(), island.getData().getTemplateFolderName(), "island");

        loadedIslands.remove(islandId);
        Rpg.log.info(island.getWorldName() + " 섬이 안전하게 저장되었습니다. (Sync)");
    }

    // --- 데이터 조회 및 나머지 메소드들 ---

    public void getOrLoadIsland(Player player, Consumer<Island> callback) {
        String islandId = plugin.getPlayerDataManager().getPlayerData(player).getIslandId();
        if (islandId == null) {
            player.sendMessage("§c당신은 섬을 소유하고 있지 않습니다.");
            if (callback != null) callback.accept(null);
            return;
        }
        if (loadedIslands.containsKey(islandId)) {
            if (callback != null) callback.accept(loadedIslands.get(islandId));
            return;
        }
        if (islandsBeingLoaded.contains(islandId)) {
            player.sendMessage("§e섬을 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
            if (callback != null) callback.accept(null);
            return;
        }
        loadIsland(islandId, callback);
    }

    public void getOrLoadIsland(String islandId, Consumer<Island> callback) {
        // 이미 로드되어 있다면, 즉시 반환
        if (loadedIslands.containsKey(islandId)) {
            callback.accept(loadedIslands.get(islandId));
            return;
        }
        // 현재 다른 곳에서 로딩 중이라면, 중복 실행 방지
        if (islandsBeingLoaded.contains(islandId)) {
            // (선택사항) 로드가 완료될 때까지 기다리게 하거나, 잠시 후 다시 시도하라고 알릴 수 있습니다.
            return;
        }

        islandsBeingLoaded.add(islandId); // 로딩 시작 표시
        IslandData islandData = dataManager.loadIslandData(islandId);
        if (islandData == null) {
            islandsBeingLoaded.remove(islandId);
            callback.accept(null);
            return;
        }

        Island island = new Island(islandData);
        WorldManager.copyAndLoadWorld(island.getWorldName(), island.getData().getTemplateFolderName(), "island", (world) -> {
            if (world != null) {
                island.setWorld(world);
                setupIslandSystems(island); // [핵심] 보관함 생성 및 자동 캐기 Task 시작
                loadedIslands.put(islandId, island);
                callback.accept(island);
            } else {
                callback.accept(null);
            }
            islandsBeingLoaded.remove(islandId); // 로딩 완료 표시 (성공/실패 무관)
        });
    }

    public Island getLoadedIsland(Player player) {
        String islandId = plugin.getPlayerDataManager().getPlayerData(player).getIslandId();
        if (islandId == null) return null;
        return loadedIslands.get(islandId);
    }

    public IslandData getIslandData(Player player) {
        String islandId = plugin.getPlayerDataManager().getPlayerData(player).getIslandId();
        if (islandId == null) return null;
        return dataManager.loadIslandData(islandId);
    }

    public IslandData getIslandDataById(String islandId) {
        if (islandId == null) return null;
        return dataManager.loadIslandData(islandId);
    }

    public Map<String, Island> getLoadedIslands() {
        return loadedIslands;
    }

    public void inviteToIsland(Player inviter, Player target) {
        IslandData islandData = getIslandData(inviter);
        if (islandData == null || !islandData.getOwner().equals(inviter.getUniqueId())) {
            inviter.sendMessage("§c섬의 주인이 아니면 초대할 수 없습니다.");
            return;
        }
        if (target == null || !target.isOnline()) {
            inviter.sendMessage("§c초대할 플레이어를 찾을 수 없습니다.");
            return;
        }
        if (plugin.getPlayerDataManager().getPlayerData(target).hasIsland()) {
            inviter.sendMessage("§c" + target.getName() + "님은 이미 섬이 있습니다.");
            return;
        }

        invites.put(target.getUniqueId(), islandData.getId());
        inviter.sendMessage("§a" + target.getName() + "님을 섬에 초대했습니다.");
        target.sendMessage("§a" + inviter.getName() + "님의 섬(" + islandData.getId() + ")에 초대되었습니다.");
        target.sendMessage("§e/섬 수락 " + inviter.getName() + " §f명령어로 수락하세요.");
    }

    public void acceptInvite(Player player, Player inviter) {
        String islandId = invites.get(player.getUniqueId());
        if (islandId == null) {
            player.sendMessage("§c받은 초대가 없습니다.");
            return;
        }

        IslandData islandData = getIslandDataById(islandId);
        if (islandData == null || !islandData.getOwner().equals(inviter.getUniqueId())) {
            player.sendMessage("§c" + inviter.getName() + "님에게 받은 섬 초대가 없습니다.");
            return;
        }

        islandData.addMember(player.getUniqueId());
        plugin.getPlayerDataManager().getPlayerData(player).setIslandId(islandData.getId());
        invites.remove(player.getUniqueId());
        saveIslandData(islandData);
        islandData.broadcast(player.getName() + "님이 섬에 합류했습니다.");
    }

    public void kickFromIsland(Player owner, Player target) {
        IslandData islandData = getIslandData(owner);
        if (islandData == null || !islandData.getOwner().equals(owner.getUniqueId())) {
            owner.sendMessage("§c섬의 주인이 아니면 추방할 수 없습니다.");
            return;
        }
        if (owner.equals(target)) {
            owner.sendMessage("§c자기 자신을 추방할 수 없습니다.");
            return;
        }
        if (!islandData.isMember(target.getUniqueId())) {
            owner.sendMessage("§c" + target.getName() + "님은 당신의 섬 멤버가 아닙니다.");
            return;
        }

        islandData.removeMember(target.getUniqueId());
        plugin.getPlayerDataManager().getPlayerData(target).setIslandId(null);
        saveIslandData(islandData);

        owner.sendMessage("§a" + target.getName() + "님을 섬에서 추방했습니다.");
        if (target.isOnline()) {
            target.sendMessage("§c" + owner.getName() + "님의 섬에서 추방당했습니다.");
        }
    }

    public void leaveIsland(Player player) {
        IslandData islandData = getIslandData(player);
        if (islandData == null) {
            player.sendMessage("§c소속된 섬이 없습니다.");
            return;
        }
        if (islandData.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§c섬의 주인은 섬을 나갈 수 없습니다. 섬을 삭제하거나 소유권을 넘겨주세요.");
            return;
        }

        islandData.broadcast(player.getName() + "님이 섬에서 나갔습니다.");
        islandData.removeMember(player.getUniqueId());
        plugin.getPlayerDataManager().getPlayerData(player).setIslandId(null);
        saveIslandData(islandData);
        player.sendMessage("§e섬에서 탈퇴했습니다.");
    }

    /**
     * [수정됨] IslandData를 파일에 저장하는 public 메소드입니다.
     * 이 섬이 현재 로드되어 있다면, 보관함 인벤토리까지 함께 저장합니다.
     * @param data 저장할 IslandData 객체
     */
    public void saveIslandData(IslandData data) {
        if (data == null) return;

        // 1. 이 IslandData에 해당하는 'Island'(활성화된 섬)가 로드되어 있는지 확인합니다.
        Island island = loadedIslands.get(data.getId());

        // 2. 만약 로드되어 있다면, 그 섬의 보관함(Inventory)을 가져옵니다.
        //    로드되어 있지 않다면, 저장할 보관함이 없으므로 null을 사용합니다.
        Inventory storage = (island != null) ? island.getStorage() : null;

        // 3. DataManager에게 IslandData와 찾은 보관함(또는 null)을 함께 넘겨 저장합니다.
        dataManager.saveIslandData(data, storage);
    }

    public boolean isIslandWorld(World world) {
        if (world == null) return false;
        return world.getName().startsWith("Island--");
    }

    private Location getSafeSpawnLocation(Island island) {
        Location spawnLoc = island.getData().getSpawnLocation();
        if (spawnLoc != null) {
            spawnLoc.setWorld(island.getWorld());
            return spawnLoc;
        }
        return island.getWorld().getSpawnLocation();
    }
}

