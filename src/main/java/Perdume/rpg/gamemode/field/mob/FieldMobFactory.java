package Perdume.rpg.gamemode.field.mob; // 사용하시는 패키지 경로

import Perdume.rpg.Rpg;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.api.mobs.MythicMob; // MythicMob import 추가
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;

import java.util.Optional;

public class FieldMobFactory {

    /**
     * [수정] 지정된 ID의 MythicMobs 몬스터를 월드에 소환하고, 그 객체를 반환합니다.
     * @param mobId MythicMobs YML 파일에 정의된 몬스터의 ID
     * @param location 소환할 위치
     * @return 소환된 ActiveMob 객체 (Optional)
     */
    public static Optional<ActiveMob> spawnMob(String mobId, Location location) {
        // [핵심] 1. MythicMobs에게 해당 ID의 '설계도(MythicMob)'가 있는지 먼저 물어봅니다.
        Optional<MythicMob> optionalMythicMob = MythicProvider.get().getMobManager().getMythicMob(mobId);

        // 2. 만약 설계도가 존재하지 않는다면, 오류를 남기고 빈 상자를 반환하여 작업을 안전하게 중단합니다.
        if (optionalMythicMob.isEmpty()) {
            Rpg.log.severe("필드 몬스터 소환 실패: MythicMobs에서 '" + mobId + "' 몹 타입을 찾을 수 없습니다! YML 파일의 mob-id를 확인해주세요.");
            return Optional.empty();
        }

        try {
            // 3. 설계도가 존재하는 것이 확인되었을 때만, 실제 몬스터(ActiveMob)를 소환합니다.
            MythicMob mythicMob = optionalMythicMob.get();
            ActiveMob spawnedMob = mythicMob.spawn(BukkitAdapter.adapt(location), 1);
            return Optional.ofNullable(spawnedMob);
        } catch (Exception e) { // InvalidMobTypeException 외의 다른 예외도 고려
            Rpg.log.severe("필드 몬스터 소환 중 알 수 없는 오류 발생: " + mobId);
            e.printStackTrace();
            return Optional.empty();
        }
    }
}