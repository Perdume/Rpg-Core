package Perdume.rpg.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.system.Portal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalListener implements Listener {

    private final Rpg plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PortalListener(Rpg plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (cooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis()) {
            return;
        }

        // --- [핵심] PortalManager에게 현재 위치에 포탈이 있는지 물어봅니다. ---
        plugin.getPortalManager().getPortalAt(event.getTo()).ifPresent(portal -> {
            
            // [디버그] 포탈을 성공적으로 찾았을 때
            Rpg.log.info("[디버그] PortalListener: " + player.getName() + "님이 포탈 '" + portal.id() + "' 범위에 진입했습니다.");
            Rpg.log.info("[디버그] -> 포탈 타입: " + portal.type() + ", 목적지: " + portal.target());
            player.sendMessage("§b포탈을 통해 이동합니다...");

            switch (portal.type()) {
                case "INSTANCED":
                    // 인스턴스 생성 로직
                    plugin.getFieldManager().enterField(player, portal);
                    break;

                case "STATIC":
                    // [핵심] 단순 텔레포트 로직
                    // 1. 포탈에 지정된 월드 이름을 기준으로 실제 월드 객체를 찾습니다.
                    World targetWorld = Bukkit.getWorld(portal.target());
                    if (targetWorld == null) {
                        player.sendMessage("§c오류: 목적지 월드('" + portal.target() + "')를 찾을 수 없습니다! 관리자에게 문의해주세요.");
                        return; // 월드가 없으면 텔레포트 중단
                    }

                    // 2. 찾아낸 월드와 포탈에 저장된 좌표 정보로 완전한 Location 객체를 생성합니다.
                    Location destination = new Location(
                            targetWorld,
                            portal.destX(),
                            portal.destY(),
                            portal.destZ(),
                            portal.destYaw(),
                            portal.destPitch()
                    );

                    player.teleport(destination);
                    break;
            }
            
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + 5000); // 5초 쿨타임
        });
    }
}