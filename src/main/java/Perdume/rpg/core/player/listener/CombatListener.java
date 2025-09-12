package Perdume.rpg.core.player.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.system.TestDummyManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class CombatListener implements Listener {

    private final Rpg plugin;

    public CombatListener(Rpg plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();

        // --- [핵심 수정] 허수아비 타격 시 대미지 측정 ---
        if (TestDummyManager.isTestDummy(victim.getUniqueId()) && damager instanceof Player attacker) {
            
            // 1. [핵심] 이벤트의 최종 피해량을 수정하기 '전에', 먼저 그 값을 읽어와 변수에 저장합니다.
            double finalDamage = event.getFinalDamage();
            
            // 2. 이제 타격감은 살리되, 실제 피해는 0으로 만들어 허수아비가 죽지 않게 합니다.
            event.setDamage(0);

            // 3. 저장해둔 최종 결과를 플레이어에게 보여줍니다.
            attacker.sendMessage(String.format("§e[대미지 측정] §f최종 대미지: §c%,.2f", finalDamage));
            return;
        }

        // --- 플레이어가 우리 NMS 보스를 공격한 '결과'를 감지 ---
        if (damager instanceof Player attacker) {
            plugin.getRaidManager().findRaidByEntityId(victim.getUniqueId()).ifPresent(raidInstance -> {
                // 이 경우에는 바닐라 대미지를 완전히 무시해야 하므로 setCancelled(true)를 사용합니다.
                event.setCancelled(true);
                
                // [핵심] AuraSkills와 다른 플러그인들이 모든 계산을 끝마친 '최종 결과'를 가져옵니다.
                double finalDamage = event.getFinalDamage();
                
                raidInstance.getBossByEntity(victim).ifPresent(bossObject -> {
                    bossObject.damage(finalDamage); // 방어무시는 외부 플러그인이 계산
                });

                // --- 타격감 효과 ---
                Location particleLoc = victim.getLocation().add(0, victim.getHeight() / 2, 0);
                victim.getWorld().playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.0f);
                // TODO: AuraSkills API를 통해 치명타가 발생했는지 확인하고 추가 이펙트 출력
            });
        }
        // --- NMS 보스가 플레이어를 공격한 '결과'를 감지 ---
        else if (victim instanceof Player playerVictim) {
            plugin.getRaidManager().findRaidByEntityId(damager.getUniqueId()).ifPresent(raidInstance -> {
                if (plugin.getRaidManager().isPlayerInRaid(playerVictim)) {
                    // 이미 모든 방어 공식이 적용된 최종 피해량에, 우리만의 '레이드 패널티'를 적용합니다.
                    double finalDamage = event.getFinalDamage() * 10.0; // 받는 대미지 10배
                    event.setDamage(finalDamage);
                }
            });
        }
    }
}