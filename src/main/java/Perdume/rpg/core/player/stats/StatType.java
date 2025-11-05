package Perdume.rpg.core.player.stats;

import org.bukkit.Material;

/**
 * [독립 버전] 우리 RPG 서버에서만 사용되는 모든 스탯의 종류와 정보를 정의하는 Enum 입니다.
 * AuraSkills 의존성이 제거되었습니다.
 */
public enum StatType {
    STRENGTH("힘", "물리 공격력, 물리 방어력, 최대 HP에 영향을 줍니다.", Material.IRON_SWORD),
    AGILITY("민첩", "공격 속도, 재사용 대기시간 감소, 이동 속도에 영향을 줍니다.", Material.FEATHER),
    INTELLIGENCE("지능", "마법 공격력, 최대 마나, 마법 저항력에 영향을 줍니다.", Material.ENCHANTING_TABLE),
    VITALITY("체력", "최대 HP, 방어력, 각종 저항력 및 회복량에 영향을 줍니다.", Material.APPLE),
    PIERCING("관통", "(구현 예정) 적의 방어력을 일정 비율 무시합니다.", Material.ARROW);

    private final String displayName;
    private final String description;
    private final Material icon;

    StatType(String displayName, String description, Material icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }
}