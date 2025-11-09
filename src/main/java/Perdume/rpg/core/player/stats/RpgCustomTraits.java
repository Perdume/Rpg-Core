package Perdume.rpg.core.player.stats;

import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.trait.CustomTrait;

/**
 * 우리 플러그인 전용 커스텀 '특성(Trait)'을 정의합니다.
 * Trait은 스탯이 실제로 수행하는 '기능'입니다.
 * [수정] PHYSICAL_DAMAGE 특성을 추가합니다.
 */
public class RpgCustomTraits {

    /**
     * '물리 공격력' 스탯이 실제로 '물리 피해'를 입히도록 하는 커스텀 특성입니다.
     * ID는 "rpg:physical_damage"가 됩니다.
     */
    public static final CustomTrait PHYSICAL_DAMAGE = CustomTrait
            .builder(NamespacedId.of("rpg", "physical_damage")) // "pluginname"을 "rpg"로 변경
            .displayName("물리 피해")
            .build();

    /**
     * '마법 공격력' 스탯이 실제로 '마법 피해'를 입히도록 하는 커스텀 특성입니다.
     * ID는 "rpg:magic_damage"가 됩니다.
     */
    public static final CustomTrait MAGIC_DAMAGE = CustomTrait
            .builder(NamespacedId.of("rpg", "magic_damage")) // "pluginname"을 "rpg"로 변경
            .displayName("마법 피해")
            .build();

    // (참고: 물리 피해는 이제 AuraSkills의 내장 Trait인 Traits.ATTACK_DAMAGE를 사용하지 않습니다.)
}