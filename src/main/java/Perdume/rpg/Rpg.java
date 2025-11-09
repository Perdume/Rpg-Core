package Perdume.rpg;


import Perdume.rpg.command.*;
import Perdume.rpg.command.admin.SpawnAdminCommand;
import Perdume.rpg.config.LocationManager;
import Perdume.rpg.core.economy.EconomyManager;
import Perdume.rpg.core.item.EnhancementManager;
import Perdume.rpg.core.item.ItemManager;
import Perdume.rpg.core.item.PotentialManager;
import Perdume.rpg.core.item.crafting.RecipeManager;
import Perdume.rpg.core.item.crafting.listener.CraftingTableListener;
import Perdume.rpg.core.item.crafting.listener.CustomCraftGUIListener;
import Perdume.rpg.core.item.crafting.listener.CustomCraftingListener;
import Perdume.rpg.core.item.crafting.listener.RecipeBookListener;
import Perdume.rpg.core.item.gui.ItemAdminGUIListener;
import Perdume.rpg.core.item.listener.BlockInteractListener;
import Perdume.rpg.core.item.listener.EnhancementGUIListener;
import Perdume.rpg.core.item.listener.EquipmentListener;
import Perdume.rpg.core.item.listener.PotentialGUIListener;
import Perdume.rpg.core.party.PartyCommand;
import Perdume.rpg.core.player.Manager.StatsManager;
import Perdume.rpg.core.player.data.PlayerDataListener;
import Perdume.rpg.core.player.data.PlayerDataManager;
import Perdume.rpg.core.player.listener.CombatListener;
import Perdume.rpg.core.player.listener.CustomDamageTraitHandler;
import Perdume.rpg.core.player.listener.RaidSessionListener;
import Perdume.rpg.core.player.listener.StatsGUIListener;
import Perdume.rpg.core.player.stats.RpgCustomStats;
import Perdume.rpg.core.player.stats.RpgCustomTraits;
import Perdume.rpg.core.reward.RewardCommand;
import Perdume.rpg.core.reward.listener.RewardClaimListener;
import Perdume.rpg.core.reward.manager.RewardManager;
import Perdume.rpg.gamemode.field.command.FieldAdminCommand;
import Perdume.rpg.gamemode.field.command.SpawnerCommand;
import Perdume.rpg.gamemode.island.listener.*;
import Perdume.rpg.gamemode.raid.RaidCommand;
import Perdume.rpg.gamemode.raid.RaidInstance;
import Perdume.rpg.gamemode.raid.listener.BossDeathListener;
import Perdume.rpg.gamemode.raid.listener.RaidGUIListener;
import Perdume.rpg.listener.FieldInstanceListener;
import Perdume.rpg.listener.PortalListener;
import Perdume.rpg.listener.SpawnGUIListener;
import Perdume.rpg.listener.WorldListener;
import Perdume.rpg.system.*;
import Perdume.rpg.world.WorldManager;
import Perdume.rpg.world.command.WorldAdminCommand;
import Perdume.rpg.world.gui.EditSessionListener;
import Perdume.rpg.world.task.EditWorldCleanupTask;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.AuraSkillsBukkit;
import dev.aurelium.auraskills.api.registry.NamespacedRegistry;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class Rpg extends JavaPlugin implements Listener {

    public static final List<String> BOSS_LIST = List.of(
            "GolemKing",
            "FireDragon" // 나중에 새로운 보스를 추가할 때 이 리스트에 ID만 추가하면 됩니다.
    );

    private static Rpg instance;
    public static Logger log;

    // --- 핵심 시스템 ---
    private RaidManager raidManager;
    private CombatListener combatListener;
    private RewardManager rewardManager;
    private PlayerDataManager playerDataManager;
    private SkyblockManager skyblockManager;
    private FieldManager fieldManager;
    private PortalManager portalManager;
    private StatsManager statsManager;
    private EconomyManager economyManager;
    private ItemManager itemManager;
    private RecipeManager recipeManager;
    private AuraSkillsApi auraSkillsApi;
    private EnhancementManager enhancementManager;
    private PotentialManager potentialManager; // [AI] 4단계
    private AuraSkillsBukkit auraSkillsBukkit;

    public static Economy econ = null;

    @Override
    public void onEnable() {
        instance = this;
        log = this.getLogger();
        this.saveResource("Reinforce.yml", false); // [AI] Reinforce.yml
        this.saveDefaultConfig();
        initializeTemplateWorlds();
        this.saveResource("items.yml", false); // [신규]
        this.saveResource("item_versions.yml", false);
        this.saveResource("enhancement.yml", false);
        this.saveResource("enhancement.yml", false); // [AI] 큐브(잠재능력) YML
        LocationManager.initialize(this);
        if (!setupEconomy()) {
            log.severe("Vault 플러그인이 없어 비활성화됩니다!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().isPluginEnabled("AuraSkills")) {
            this.auraSkillsApi = AuraSkillsApi.get();
            this.auraSkillsBukkit = AuraSkillsBukkit.get(); // [AI] 2.x API
        } else {
            log.severe("AuraSkills 플러그인이 없어 비활성화됩니다!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.economyManager = new EconomyManager();
        if (!economyManager.setupEconomy()) getLogger().severe("Vault 또는 경제 플러그인을 찾을 수 없습니다! 경제 관련 기능이 비활성화됩니다.");
        else getLogger().info("Vault와 성공적으로 연동되었습니다.");
        this.playerDataManager = new PlayerDataManager(this);
        getServer().getPluginManager().registerEvents(this, this);
        initializeSystems();
        registerCommandsAndSystems();
        new PortalParticleTask(this).runTaskTimer(this, 0L, 1L);
        itemManager.loadItems();
        recipeManager.loadRecipes();
        registerCustomStats();

        log.info("RPG Plugin이 성공적으로 활성화되었습니다.");
    }

    /**
     * [최종 수정] 플러그인이 비활성화될 때, 모든 데이터를 안전하게 저장하고 세션을 종료합니다.
     */
    @Override
    public void onDisable() {
        log.info("플러그인 비활성화를 시작합니다. 모든 데이터를 안전하게 저장합니다...");

        // 1. 모든 보상 데이터를 파일에 저장합니다.
        if (rewardManager != null) {
            rewardManager.saveRewards();
            log.info("- 보상 우편함 데이터를 저장했습니다.");
        }

        // 2. 모든 맵 수정 세션을 강제로 저장하고 정리합니다.
        WorldAdminCommand worldAdminCommand = (WorldAdminCommand) getCommand("rpworld").getExecutor();
        if (worldAdminCommand != null && !WorldAdminCommand.editingPlayers.isEmpty()) {
            log.info(WorldAdminCommand.editingPlayers.size() + "개의 맵 수정 세션을 강제 저장합니다...");
            // ConcurrentModificationException 방지를 위해 키 목록의 복사본을 만들어 순회합니다.
            for (UUID uuid : new ArrayList<>(WorldAdminCommand.editingPlayers.keySet())) {
                Player p = getServer().getPlayer(uuid);
                if (p != null) {
                    // 저장 후 콜백에서 로그를 남기도록 할 수 있으나, 여기서는 즉시 처리합니다.
                    worldAdminCommand.handleSave(p, (success) -> {
                        if(success) {
                            log.info("- " + p.getName() + "님의 맵 수정 작업을 저장했습니다.");
                        } else {
                            log.warning("- " + p.getName() + "님의 맵 수정 작업 저장에 실패했습니다.");
                        }
                    });
                }
            }
        }

        // 3. 현재 접속 중인 모든 플레이어의 데이터를 저장합니다.
        if (playerDataManager != null) {
            log.info(getServer().getOnlinePlayers().size() + "명의 온라인 플레이어 데이터를 저장합니다...");
            for (Player player : getServer().getOnlinePlayers()) {
                playerDataManager.savePlayerDataOnQuit(player);
            }
        }

        // 4. [핵심] 현재 로드된 모든 스카이블럭 섬을 저장하고 언로드합니다.
        if (skyblockManager != null && !skyblockManager.getLoadedIslands().isEmpty()) {
            log.info("[Rpg] " + skyblockManager.getLoadedIslands().size() + "개의 활성화된 섬을 저장합니다...");

            for (String islandId : new ArrayList<>(skyblockManager.getLoadedIslands().keySet())) {
                skyblockManager.unloadIslandSync(islandId);
            }
            log.info("[Rpg] 모든 활성화된 섬 저장이 완료되었습니다.");
        }

        // 5. 모든 레이드 인스턴스를 강제로 종료합니다.
        if (raidManager != null && !raidManager.getActiveRaids().isEmpty()) {
            log.info(raidManager.getActiveRaids().size() + "개의 레이드를 강제 종료합니다...");
            // activeRaids 리스트를 직접 수정하면 오류가 발생하므로, 복사본을 만들어 순회합니다.
            for (RaidInstance instance : new ArrayList<>(raidManager.getActiveRaids())) {
                instance.end(false); // 실패 처리로 강제 종료
            }
        }

        log.info("RPG Plugin이 비활성화되었습니다.");
    }

    // --- 플레이어 데이터 관리 이벤트 ---
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerDataManager.loadPlayerDataOnJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataManager.savePlayerDataOnQuit(event.getPlayer());
    }

    // Getter 추가
    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }

    private void initializeTemplateWorlds() {
        log.info("필수 템플릿 월드 초기화를 시작합니다...");

        // --- 1. 레이드 보스 템플릿 확인 ---
        FileConfiguration config = getConfig();
        List<String> bossIds = config.getStringList("raid-bosses");

        if (bossIds.isEmpty()) {
            log.warning("'config.yml'에 raid-bosses 목록이 비어있습니다.");
        } else {
            log.info("레이드 템플릿 월드를 확인합니다...");
            for (String bossId : bossIds) {
                // "raid" 타입으로 공허 템플릿 생성 시도
                if (WorldManager.createVoidTemplate(bossId, "raid")) {
                    log.info("- '" + bossId + "' 템플릿 월드 확인/생성 완료.");
                } else {
                    log.severe("- '" + bossId + "' 템플릿 월드 생성 실패!");
                }
            }
        }

        // --- 2. 스카이블럭 '기본 섬' 템플릿 확인 ---
        log.info("스카이블럭 템플릿 월드를 확인합니다...");
        String islandTemplateName = "island_template";
        // "island" 타입으로 공허 템플릿 생성 시도
        if (WorldManager.createVoidTemplate(islandTemplateName, "island")) {
            log.info("- '" + islandTemplateName + "' 템플릿 월드 확인/생성 완료.");
        } else {
            log.severe("- '" + islandTemplateName + "' 템플릿 월드 생성 실패!");
        }

        log.info("모든 템플릿 월드 초기화가 완료되었습니다.");
    }



    private void initializeSystems() {
        this.raidManager = new RaidManager(this);
        this.combatListener = new CombatListener(this);
        this.rewardManager = new RewardManager(this);
        this.skyblockManager = new SkyblockManager(this);
        this.fieldManager = new FieldManager(this); // [신규] FieldManager 초기화
        this.portalManager = new PortalManager(this);
        this.statsManager = new StatsManager(this);
        this.itemManager = new ItemManager(this, this.auraSkillsApi, this.auraSkillsBukkit);
        this.recipeManager = new RecipeManager(this);
        this.statsManager = new StatsManager(this);
        this.enhancementManager = new EnhancementManager(this); // [신규]
        this.potentialManager = new PotentialManager(this);
        this.enhancementManager = new EnhancementManager(this);
    }

    private void cleanupTemporaryWorlds() {
        log.info("임시 월드 청소를 시작합니다...");
        File worldContainer = getServer().getWorldContainer();
        if (worldContainer == null || !worldContainer.isDirectory()) return;

        File[] worldFolders = worldContainer.listFiles();
        if (worldFolders == null) return;

        int count = 0;
        for (File worldFolder : worldFolders) {
            String folderName = worldFolder.getName();
            if (worldFolder.isDirectory() && (folderName.startsWith("Raid--RUN--") || folderName.startsWith("Raid--EDIT--"))) {
                if (getServer().getWorld(folderName) != null) {
                    getServer().unloadWorld(folderName, false);
                }
                WorldManager.deleteWorldFolder(worldFolder);
                count++;
            }
        }
        if (count > 0) {
            log.info(count + "개의 임시 월드를 성공적으로 삭제했습니다.");
        }
    }
    /**
     * 플러그인의 모든 명령어 실행기와 이벤트 리스너를 서버에 등록합니다.
     * 각 시스템별로 구역을 나누어 가독성과 유지보수성을 높였습니다.
     */
    private void registerCommandsAndSystems() {

        // --- 파티 및 레이드 시스템 ---
        getCommand("party").setExecutor(new PartyCommand());
        getCommand("raid").setExecutor(new RaidCommand(this));
        getServer().getPluginManager().registerEvents(new RaidGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new RaidSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new BossDeathListener(this), this);

        // --- 보상 시스템 ---
        getCommand("보상").setExecutor(new RewardCommand());
        getServer().getPluginManager().registerEvents(new RewardClaimListener(), this);

        // --- 월드 관리 시스템 ---
        WorldAdminCommand worldAdminCommand = new WorldAdminCommand(this);
        getCommand("rpworld").setExecutor(worldAdminCommand);
        getCommand("rpworld").setTabCompleter(worldAdminCommand);
        getServer().getPluginManager().registerEvents(new EditSessionListener(this, worldAdminCommand), this);
        new EditWorldCleanupTask(this, worldAdminCommand).runTaskTimer(this, 0L, 6000L);

        getCommand("스폰").setExecutor(new SpawnCommand()); // /스폰 명령어 등록
        getServer().getPluginManager().registerEvents(new SpawnGUIListener(), this); // GUI 리스너 등록

        getCommand("spawnset").setExecutor(new SpawnAdminCommand()); // [추가]
        getServer().getPluginManager().registerEvents(new WorldListener(this), this); // [추가]

        // --- 테스트 시스템 ---
        getCommand("rpgtest").setExecutor(new TestCommand(this));

        getCommand("fieldadmin").setExecutor(new FieldAdminCommand(this)); // [신규]

        getCommand("스포너분석").setExecutor(new SpawnerCommand());
        getCommand("스탯").setExecutor(new StatsCommand(this));
        getCommand("rpgitem").setExecutor(new ItemAdminCommand(this));

        // --- 핵심 리스너 등록 ---
        getServer().getPluginManager().registerEvents(this.combatListener, this);
        getServer().getPluginManager().registerEvents(new IslandSettingsGUIListener(skyblockManager), this); // [추가]
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);
        getServer().getPluginManager().registerEvents(new IslandWorldListener(this), this); // [추가]
        getServer().getPluginManager().registerEvents(new CobblestoneGeneratorListener(this), this); // [추가]
        getServer().getPluginManager().registerEvents(new PortalListener(this), this); // [신규]
        getServer().getPluginManager().registerEvents(new FieldInstanceListener(this), this); // [신규]
        getServer().getPluginManager().registerEvents(new IslandMiningListener(skyblockManager), this);
        getServer().getPluginManager().registerEvents(new IslandStorageGUIListener(), this); // [신규]
        getServer().getPluginManager().registerEvents(new StatsGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomCraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingTableListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomCraftGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new RecipeBookListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomCraftGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomCraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new RecipeBookListener(this), this);
        getServer().getPluginManager().registerEvents(new EnhancementGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PotentialGUIListener(this), this); // [AI] 4단계
        getServer().getPluginManager().registerEvents(new EquipmentListener(this), this); // [AI] EquipmentListener 등록
        getServer().getPluginManager().registerEvents(new ItemAdminGUIListener(this), this);
        // getServer().getPluginManager().registerEvents(new GlobalRespawnListener(this), this); // 필요 시 활성화

        // --- ISLAND ---
        IslandCommand ic = new IslandCommand(this);
        getCommand("섬").setExecutor(ic); // /섬 명령어 등록
        getCommand("섬").setTabCompleter(ic);
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    /**
     * [신규] AuraSkills에 우리가 만든 스탯과 특성을 등록합니다.
     */
    private void registerCustomStats() {
        if (this.auraSkillsApi == null) return;

        // "rpg"라는 이름공간(pluginname)으로 레지스트리를 사용합니다.
        NamespacedRegistry registry = auraSkillsApi.useRegistry("rpg", getDataFolder());

        // 1. 특성(기능) 등록
        registry.registerTrait(RpgCustomTraits.PHYSICAL_DAMAGE); // [수정] 물리 특성 등록
        registry.registerTrait(RpgCustomTraits.MAGIC_DAMAGE);

        // 2. 스탯(항목) 등록
        registry.registerStat(RpgCustomStats.PHYSICAL_POWER);
        registry.registerStat(RpgCustomStats.MAGIC_POWER);

        // 3. 특성 핸들러(실제 구현) 등록
        // [수정] MagicDamageTraitHandler -> CustomDamageTraitHandler
        auraSkillsApi.getHandlers().registerTraitHandler(new CustomDamageTraitHandler(this, this.auraSkillsApi));

        log.info("AuraSkills에 '물리 공격력' 및 '마법 공격력' 커스텀 스탯/특성을 성공적으로 등록했습니다.");
    }



    // --- Getter 메소드 ---
    public static Rpg getInstance() { return instance; }
    public RaidManager getRaidManager() { return raidManager; }
    public CombatListener getCombatListener() {return combatListener;}
    public RewardManager getRewardManager() {return rewardManager;}
    public SkyblockManager getSkyblockManager() {
        return this.skyblockManager;
    }
    public FieldManager getFieldManager() {return this.fieldManager;}
    public PortalManager getPortalManager() {return this.portalManager;}
    public StatsManager getStatsManager() {return statsManager; }
    public EconomyManager getEconomyManager() {return economyManager;}
    public ItemManager getItemManager() {
        return itemManager;
    }
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
    // [신규] StatsManager 등이 AuraSkills API에 접근할 수 있도록 Getter 추가
    public AuraSkillsApi getAuraSkillsApi() {
        return this.auraSkillsApi;
    }
    public EnhancementManager getEnhancementManager() {
        return this.enhancementManager;
    }
    public AuraSkillsBukkit getAuraSkillsBukkit() {
        return this.auraSkillsBukkit;
    }
    public PotentialManager getPotentialManager() { // [AI] 4단계
        return this.potentialManager;
    }
}
