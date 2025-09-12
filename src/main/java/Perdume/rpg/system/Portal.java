package Perdume.rpg.system;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

// [수정] Portal은 이제 자신이 속한 '월드 이름'과 목적지 '템플릿'을 함께 기억합니다.
public record Portal(
        String id,
        BoundingBox region,
        String worldName, // 포탈이 위치한 월드의 이름 (또는 템플릿 이름)
        String type,      // "INSTANCED" 또는 "STATIC"
        String target,    // 목적지 월드 이름 (또는 템플릿 이름)
        double destX, double destY, double destZ,
        float destYaw, float destPitch
){}