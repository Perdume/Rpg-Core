package Perdume.rpg.gamemode.field.command;

import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.field.util.SpawnFinderUtil;
import Perdume.rpg.world.command.WorldAdminCommand;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector; // Vector import

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnerCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { /* ... */ return true; }
        if (!player.hasPermission("rpg.admin")) { /* ... */ return true; }

        if (args.length < 4) {
            player.sendMessage("§c사용법: /스포너분석 <몹ID> <최대개체수> <리스폰시간(초)> <반경>");
            return true;
        }

        String worldName = player.getWorld().getName();
        if (!worldName.startsWith("Field--EDIT--")) {
            player.sendMessage("§c이 명령어는 /wa edit field <맵이름> 으로 진입한 곳에서만 사용할 수 있습니다.");
            return true;
        }

        var session = WorldAdminCommand.editingPlayers.get(player.getUniqueId());
        if (session == null || !session.type().equals("field")) {
            player.sendMessage("§c'field' 타입의 월드에서만 사용할 수 있습니다.");
            return true;
        }
        String templateName = session.templateName();

        try {
            String mobId = args[0];
            int maxMobs = Integer.parseInt(args[1]);
            int respawnTime = Integer.parseInt(args[2]);
            int radius = Integer.parseInt(args[3]);

            player.sendMessage("§e" + radius + "칸 반경의 스폰 지점 분석을 시작합니다...");
            List<Location> foundLocations = SpawnFinderUtil.findSpawnableLocations(player.getLocation(), radius);

            if (foundLocations.isEmpty()) {
                player.sendMessage("§c스폰 가능한 지점을 찾지 못했습니다.");
                return true;
            }

            // [핵심] Location 리스트를 Vector 리스트로 변환하여, 월드 정보를 제거합니다.
            List<Vector> spawnVectors = foundLocations.stream()
                                                      .map(Location::toVector)
                                                      .collect(Collectors.toList());

            File configFile = new File(Rpg.getInstance().getDataFolder(), "worlds/field/" + templateName + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            config.set("mob-id", mobId);
            config.set("max-mobs", maxMobs);
            config.set("respawn-time", respawnTime);
            config.set("spawn-points", spawnVectors); // Vector 리스트를 저장

            config.save(configFile);
            player.sendMessage("§a성공적으로 " + spawnVectors.size() + "개의 스폰 지점을 '" + templateName + ".yml' 파일에 저장했습니다.");

        } catch (Exception e) {
            player.sendMessage("§c명령어 인자가 잘못되었습니다. 숫자를 확인해주세요.");
        }
        return true;
    }
}