package Perdume.rpg.command; // 사용하시는 패키지 경로

import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.island.gui.IslandSettingsGUI;
import Perdume.rpg.system.SkyblockManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// [핵심] CommandExecutor와 TabCompleter를 모두 구현(implements)합니다.
public class IslandCommand implements CommandExecutor, TabCompleter {

    private final Rpg plugin;
    private final SkyblockManager skyblockManager;

    public IslandCommand(Rpg plugin) {
        this.plugin = plugin;
        this.skyblockManager = plugin.getSkyblockManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }

        if (args.length == 0) {
            // 인자가 없으면 자기 섬으로 이동
            skyblockManager.enterMyIsland(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "생성", "create" -> skyblockManager.createIsland(player);
            // [핵심] "설정" 파라미터 추가
            case "설정", "settings", "s" -> IslandSettingsGUI.open(player);
            case "초대", "invite" -> {
                if (args.length < 2) { player.sendMessage("§c사용법: /섬 초대 <플레이어이름>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                skyblockManager.inviteToIsland(player, target);
            }
            case "수락", "accept" -> {
                if (args.length < 2) { player.sendMessage("§c사용법: /섬 수락 <섬주인이름>"); return true; }
                Player inviter = Bukkit.getPlayer(args[1]);
                skyblockManager.acceptInvite(player, inviter);
            }
            case "추방", "kick" -> {
                if (args.length < 2) { player.sendMessage("§c사용법: /섬 추방 <플레이어이름>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                skyblockManager.kickFromIsland(player, target);
            }
            case "나가기", "leave" -> skyblockManager.leaveIsland(player);
            case "정보", "info" -> skyblockManager.showIslandInfo(player);
            case "홈설정", "sethome" -> skyblockManager.setHome(player);
            default -> sendHelp(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        // 첫 번째 인자 (서브 명령어) 자동 완성
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0],
                    Arrays.asList("생성", "초대", "수락", "추방", "나가기", "정보", "홈설정"), completions);
        }
        // 두 번째 인자 (플레이어 이름) 자동 완성
        else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            // 초대, 수락, 추방 명령어의 경우 온라인 플레이어 목록을 추천
            if (subCommand.equals("초대") || subCommand.equals("수락") || subCommand.equals("추방")) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> !name.equals(player.getName())) // 자기 자신은 제외
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
            }
        }
        return completions;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6--- 섬 명령어 (/섬 또는 /is) ---");
        player.sendMessage("§e/섬 §7- 자신의 섬으로 이동합니다.");
        player.sendMessage("§e/섬 생성 §7- 새로운 섬을 생성합니다.");
        player.sendMessage("§e/섬 초대 <이름> §7- 플레이어를 자신의 섬에 초대합니다. (섬주인 전용)");
        player.sendMessage("§e/섬 수락 <이름> §7- 받은 섬 초대를 수락합니다.");
        player.sendMessage("§e/섬 추방 <이름> §7- 섬에서 팀원을 추방합니다. (섬주인 전용)");
        player.sendMessage("§e/섬 나가기 §7- 현재 소속된 섬에서 나갑니다.");
        player.sendMessage("§e/섬 정보 §7- 현재 섬의 정보를 봅니다.");
        player.sendMessage("§e/섬 홈설정 §7- 섬의 스폰 위치를 현재 위치로 설정합니다.");
    }
}