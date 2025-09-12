package Perdume.rpg.system;

import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.field.HuntingGroundInstance;
import Perdume.rpg.world.WorldManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class FieldManager {

    private final Rpg plugin;
    private final Map<String, HuntingGroundInstance> activeFields = new HashMap<>();
    private final Map<UUID, HuntingGroundInstance> playerLocations = new HashMap<>();
    private int nextFieldId = 1;

    public FieldManager(Rpg plugin) {
        this.plugin = plugin;
    }

    /**
     * [신규] 개인 플레이어를 위한 사냥터 인스턴스를 생성하고 입장시킵니다. (포탈 사용 시)
     * @param player 입장할 플레이어
     * @param portal 사용된 포탈의 정보
     */
    public void enterField(Player player, Portal portal) {
        String templateName = portal.target();
        // [핵심] 실제 인스턴스 생성 로직을 새로운 공통 메소드로 분리
        createAndEnterInstance(player, templateName, (world) -> {
            // 포탈에 지정된 목적지 좌표로 텔레포트
            Location destination = new Location(world, portal.destX(), portal.destY(), portal.destZ(), portal.destYaw(), portal.destPitch());
            player.teleport(destination);
        });
    }

    /**
     * 관리자가 디버그용으로 사냥터에 입장할 때 사용합니다.
     * @param player 입장할 관리자
     * @param templateName 입장할 사냥터 템플릿의 이름
     */
    public void enterField(Player player, String templateName) {
        // [핵심] 실제 인스턴스 생성 로직을 새로운 공통 메소드로 분리
        createAndEnterInstance(player, templateName, (world) -> {
            // 관리자는 월드의 기본 스폰 지점(0, 65, 0)으로 텔레포트
            player.teleport(world.getSpawnLocation());
        });
    }

    /**
     * 중복되는 인스턴스 생성 및 입장 로직을 하나로 통합한 내부 메소드
     */
    private void createAndEnterInstance(Player player, String templateName, Consumer<World> onEnter) {
        if (playerLocations.containsKey(player.getUniqueId())) {
            player.sendMessage("§c이미 다른 사냥터에 입장해 있습니다.");
            return;
        }

        File templateFolder = new File(plugin.getDataFolder(), "worlds/field/" + templateName);
        if (!templateFolder.exists()) {
            player.sendMessage("§c존재하지 않는 사냥터입니다: " + templateName);
            return;
        }

        String instanceWorldName = "Field--" + templateName + "--" + player.getName();

        WorldManager.copyAndLoadWorld(instanceWorldName, templateName, "field", (world) -> {
            if (world == null) {
                player.sendMessage("§c사냥터 입장에 실패했습니다.");
                return;
            }
            HuntingGroundInstance instance = new HuntingGroundInstance(plugin, world, templateName);
            activeFields.put(instanceWorldName, instance);
            playerLocations.put(player.getUniqueId(), instance);

            if (player.isOnline()) {
                onEnter.accept(world); // 텔레포트 로직 실행
            }
        });
    }

    /**
     * 플레이어가 사냥터를 떠날 때 호출됩니다.
     */
    public void leaveField(Player player) {
        HuntingGroundInstance instance = playerLocations.remove(player.getUniqueId());
        if (instance == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (instance.getWorld() != null && instance.getWorld().getPlayers().isEmpty()) {
                    unloadField(instance.getWorld().getName());
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * 특정 사냥터 인스턴스를 저장 없이 언로드하고 정리합니다.
     */
    public void unloadField(String worldName) {
        HuntingGroundInstance instance = activeFields.remove(worldName);
        if (instance != null) {
            instance.cleanup();
            WorldManager.unloadAnddeleteWorld(worldName);
            Rpg.log.info("사냥터 인스턴스(" + worldName + ")가 비어있어 성공적으로 언로드되었습니다.");
        }
    }
}