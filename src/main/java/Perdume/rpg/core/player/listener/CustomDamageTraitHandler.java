package Perdume.rpg.core.player.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.CustomItemUtil;
import Perdume.rpg.core.player.stats.RpgCustomTraits;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.bukkit.BukkitTraitHandler;
import dev.aurelium.auraskills.api.trait.Trait;
import dev.aurelium.auraskills.api.user.SkillsUser;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * [수정] RpgCustomTraits.PHYSICAL_DAMAGE와 RpgCustomTraits.MAGIC_DAMAGE
 * 특성의 실제 기능을 모두 구현하는 '통합 핸들러' 클래스입니다.
 */
public class CustomDamageTraitHandler implements BukkitTraitHandler, Listener {

    private final Rpg plugin;
    private final AuraSkillsApi auraSkillsApi;

    public CustomDamageTraitHandler(Rpg plugin, AuraSkillsApi auraSkillsApi) {
        this.plugin = plugin;
        this.auraSkillsApi = auraSkillsApi;
    }

    @Override
    public Trait[] getTraits() {
        // [수정] 이 핸들러가 '물리 피해'와 '마법 피해' 특성을 모두 담당함을 알립니다.
        return new Trait[] { 
            RpgCustomTraits.PHYSICAL_DAMAGE, 
            RpgCustomTraits.MAGIC_DAMAGE 
        };
    }

    /**
     * 플레이어가 공격할 때, 무기를 판별하고 대미지를 직접 적용합니다.
     * (AuraSkills의 '물리 공격' 로직보다 먼저 실행되어야 함)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCustomAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // 1. 공격자가 '마법 무기'를 들고 있는지 확인합니다.
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        String itemId = CustomItemUtil.getItemId(weapon); //
        
        boolean isMagicAttack = (itemId != null && (itemId.contains("staff") || itemId.contains("wand")));
        
        SkillsUser user = auraSkillsApi.getUser(attacker.getUniqueId());
        double finalDamage = 0;

        if (isMagicAttack) {
            // 2. [마법 공격] 'MAGIC_DAMAGE' 특성 레벨(총 마법 공격력)을 가져옵니다.
            finalDamage = user.getEffectiveTraitLevel(RpgCustomTraits.MAGIC_DAMAGE);
        } else {
            // 3. [물리 공격] 'PHYSICAL_DAMAGE' 특성 레벨(총 물리 공격력)을 가져옵니다.
            finalDamage = user.getEffectiveTraitLevel(RpgCustomTraits.PHYSICAL_DAMAGE);
        }

        // 4. AuraSkills의 기본 물리 대미지 계산을 '무시'시킵니다.
        event.setDamage(0); 

        // 5. 방어력 등을 무시하고 '순수 피해'를 입힙니다.
        //    (나중에 여기에 victim의 '방어력'을 계산하는 로직을 추가할 수 있습니다)
        event.setDamage(finalDamage);
    }

    // --- API 문서의 나머지 필수 메소드들 ---
    @Override
    public double getBaseLevel(Player player, Trait trait) {
        // [수정] 두 특성 모두 기본 피해는 0
        return 0; 
    }
    @Override
    public void onReload(Player player, SkillsUser user, Trait trait) {
        // 스탯이 변경될 때마다 호출됨 (예: 장비 착용/해제)
    }
}