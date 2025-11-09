package Perdume.rpg.core.player.stats;

import dev.aurelium.auraskills.api.item.ItemContext;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.stat.CustomStat;
// [수정] import dev.aurelium.auraskills.api.trait.Traits; // (더 이상 사용 안함)
// [수정] import dev.aurelium.auraskills.api.stat.StatUnit; // (잘못된 문법이므로 삭제)

/**
 * 우리 플러그인 전용 커스텀 '스탯(Stat)'을 정의합니다.
 * [수정] PHYSICAL_POWER가 Traits.ATTACK_DAMAGE 대신 RpgCustomTraits.PHYSICAL_DAMAGE를 올리도록 변경
 * [수정] 잘못된 .unit() 문법 삭제
 */
public class RpgCustomStats {

    /**
     * 물리 공격력 스탯입니다. (ID: "rpg:physical_power")
     * [수정] 이 스탯 1은 우리가 만든 'PHYSICAL_DAMAGE' 커스텀 특성 1을 올립니다.
     */
    public static final CustomStat PHYSICAL_POWER = CustomStat
            .builder(NamespacedId.of("rpg", "physical_power"))
            .trait(RpgCustomTraits.PHYSICAL_DAMAGE, 1.0) // [수정] Traits.ATTACK_DAMAGE -> RpgCustomTraits.PHYSICAL_DAMAGE
            // .unit(StatUnit.ABSOLUTE) // [수정] 이 줄은 잘못된 문법이므로 삭제
            .displayName("§c물리 공격력")
            .description("물리 공격력이 증가합니다.")
            .color("<red>")
            .symbol("§c⚔")
            .item(ItemContext.builder()
                    .material("iron_sword")
                    .group("lower") // /skills stat GUI의 하단 그룹
                    .order(1)       // 하단 그룹의 첫 번째
                    .build())
            .build();

    /**
     * 마법 공격력 스탯입니다. (ID: "rpg:magic_power")
     * 이 스탯 1은 우리가 만든 'MAGIC_DAMAGE' 커스텀 특성 1을 올립니다.
     */
    public static final CustomStat MAGIC_POWER = CustomStat
            .builder(NamespacedId.of("rpg", "magic_power"))
            .trait(RpgCustomTraits.MAGIC_DAMAGE, 1.0) // 1 스탯 = 1 마법 피해
            // .unit(StatUnit.ABSOLUTE) // [수정] 이 줄은 잘못된 문법이므로 삭제
            .displayName("§b마법 공격력")
            .description("마법 공격력이 증가합니다.")
            .color("<aqua>")
            .symbol("§b✨")
            .item(ItemContext.builder()
                    .material("enchanting_table")
                    .group("lower") // /skills stat GUI의 하단 그룹
                    .order(2)       // 하단 그룹의 두 번째
                    .build())
            .build();
}