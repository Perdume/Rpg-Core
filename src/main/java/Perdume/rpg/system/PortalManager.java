package Perdume.rpg.system;

import Perdume.rpg.Rpg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BoundingBox;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PortalManager {
    private final Rpg plugin;
    private final List<Portal> portals = new ArrayList<>();

    public PortalManager(Rpg plugin) {
        this.plugin = plugin;
        loadPortals();
    }

    public void loadPortals() {
        portals.clear();
        File portalsFile = new File(plugin.getDataFolder(), "portals.yml");
        if (!portalsFile.exists()) plugin.saveResource("portals.yml", false);

        FileConfiguration config = YamlConfiguration.loadConfiguration(portalsFile);
        ConfigurationSection section = config.getConfigurationSection("portals");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String type = section.getString(key + ".type", "STATIC").toUpperCase();
                    String pos1String = section.getString(key + ".pos1");
                    String pos2String = section.getString(key + ".pos2");
                    String destString = section.getString(key + ".destination");

                    String[] pos1Parts = pos1String.split(", ");
                    String[] pos2Parts = pos2String.split(", ");
                    if (!pos1Parts[0].equals(pos2Parts[0])) throw new IllegalArgumentException("pos1과 pos2의 월드가 다릅니다.");

                    BoundingBox region = BoundingBox.of(
                        new Location(null, Double.parseDouble(pos1Parts[1]), Double.parseDouble(pos1Parts[2]), Double.parseDouble(pos1Parts[3])),
                        new Location(null, Double.parseDouble(pos2Parts[1]), Double.parseDouble(pos2Parts[2]), Double.parseDouble(pos2Parts[3]))
                    );
                    String worldName = pos1Parts[0];

                    String[] destParts = destString.split(", ");
                    String targetName = destParts[0];
                    double dX = Double.parseDouble(destParts[1]), dY = Double.parseDouble(destParts[2]), dZ = Double.parseDouble(destParts[3]);
                    float dYaw = Float.parseFloat(destParts[4]), dPitch = Float.parseFloat(destParts[5]);

                    portals.add(new Portal(key, region, worldName, type, targetName, dX, dY, dZ, dYaw, dPitch));

                } catch (Exception e) {
                    Rpg.log.warning("포탈 '" + key + "' 로드 실패: " + e.getMessage());
                    Rpg.log.warning("[디버그] 포탈 '" + key + "' 로드 중 오류 발생: " + e.getMessage());
                }
            }
            Rpg.log.info(portals.size() + "개의 포탈을 성공적으로 불러왔습니다.");
        }
    }

    public Optional<Portal> getPortalAt(Location location) {
        return portals.stream().filter(portal -> {
            String currentWorldName = location.getWorld().getName();
            String portalWorldName = portal.worldName();

            // 1. 월드 이름이 정확히 일치하거나 (예: "world"),
            // 2. 현재 인스턴스 월드 이름에 포탈의 템플릿 이름이 포함되어 있을 때 (예: "Field--OrcValley--...")
            boolean worldMatches = currentWorldName.equals(portalWorldName) || currentWorldName.contains("--" + portalWorldName + "--");

            return worldMatches && portal.region().contains(location.toVector());
        }).findFirst();
    }
    /**
     * [신규] 특정 월드에 속한 모든 포탈의 목록을 반환합니다.
     * PortalParticleTask가 이 메소드를 사용하여 파티클을 생성할 위치를 찾습니다.
     */
    public List<Portal> getPortalsInWorld(World world) {
        String worldName = world.getName();
        return portals.stream()
                .filter(portal -> worldName.equals(portal.worldName()) || worldName.contains("--" + portal.worldName() + "--"))
                .collect(Collectors.toList());
    }
    // [New Method] Returns a list of all portal regions
    public List<Portal> getAllPortals() {
        return new ArrayList<>(portals);
    }
}