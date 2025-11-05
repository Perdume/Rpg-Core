package Perdume.rpg.gamemode.island.upgrade;

/**
 * 섬의 자동 채굴 및 기타 기능 업그레이드에 대한 모든 정보를 중앙에서 관리하는 Enum 클래스입니다.
 * 각 업그레이드의 이름과 티어별 수치를 정의합니다.
 */
public enum IslandUpgrade {

    // --- 업그레이드 종류 및 티어별 값 정의 ---
    AUTO_MINER("생산 주기", 15.0, 12.0, 9.0, 6.0, 3.0),         // 단위: 초
    RARE_DROP("희귀 드랍 보정", 0.0, 0.05, 0.1, 0.2, 0.5),     // 단위: %
    MULTI_DROP("멀티드랍 확률", 0.0, 5.0, 10.0, 20.0, 35.0),    // 단위: % (T5의 3배 드랍은 로직에서 별도 처리)
    STORAGE("보관함 확장", 1.0, 2.0, 3.0, 4.0, 5.0);           // 단위: 줄

    private final String upgradeName;
    private final double[] valuesByTier; // 티어별 수치를 저장하는 배열

    /**
     * IslandUpgrade Enum 생성자
     * @param name 업그레이드의 표시 이름
     * @param values 1티어부터 순서대로 적용될 값들
     */
    IslandUpgrade(String name, double... values) {
        this.upgradeName = name;
        this.valuesByTier = new double[values.length + 1];
        for (int i = 0; i < values.length; i++) {
            this.valuesByTier[i + 1] = values[i];
        }
    }

    public String getUpgradeName() {
        return upgradeName;
    }

    /**
     * 특정 티어에 해당하는 업그레이드 수치를 반환합니다.
     * @param tier 조회할 티어 (1-5)
     * @return 해당 티어의 값
     */
    public double getValue(int tier) {
        if (tier < 1 || tier >= valuesByTier.length) {
            return valuesByTier[1]; // 범위를 벗어나면 1티어 값 반환
        }
        return valuesByTier[tier];
    }
}
