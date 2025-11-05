package Perdume.rpg.command;

import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.island.Island;
import Perdume.rpg.gamemode.island.IslandData;
import Perdume.rpg.gamemode.island.gui.IslandUpgradeGUI;
import Perdume.rpg.system.SkyblockManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [TabCompleter 포함] 섬 관련 모든 사용자 명령어를 처리하는 클래스입니다.
 * CommandExecutor와 TabCompleter를 모두 구현합니다.
 */
public class IslandCommand implements CommandExecutor, TabCompleter {

    private final Rpg plugin;
    private final SkyblockManager skyblockManager;

    public IslandCommand(Rpg plugin) {
        this.plugin = plugin;
        this.skyblockManager = plugin.getSkyblockManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            skyblockManager.teleportToIsland(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // --- 월드 로드가 '필수'인 명령어 (섬 내부에서만 사용) ---
        if (Arrays.asList("업그레이드", "upgrade", "홈", "sethome", "창고", "보관함", "storage").contains(subCommand)) {
            Island island = skyblockManager.getLoadedIsland(player);
            if (island == null) {
                player.sendMessage("§c이 명령어는 당신의 섬 안에서만 사용할 수 있습니다. /섬 명령어로 먼저 입장해주세요.");
                return true;
            }
            handleLoadedIslandCommands(player, island, subCommand, args);
            return true;
        }

        // --- 월드 로드가 '필요 없는' 데이터 기반 명령어 (어디서든 사용) ---
        handleDataCommands(player, subCommand, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // 첫 번째 인자에 대한 명령어 목록 자동완성
            return StringUtil.copyPartialMatches(args[0],
                    Arrays.asList("생성", "정보", "초대", "수락", "추방", "나가기", "업그레이드", "창고", "홈"),
                    new ArrayList<>());
        } else if (args.length == 2) {
            // 두 번째 인자에 대한 자동완성
            String subCommand = args[0].toLowerCase();
            // 초대, 수락, 추방 명령어의 경우 온라인 플레이어 목록을 보여줌
            if (Arrays.asList("초대", "수락", "추방").contains(subCommand)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> StringUtil.startsWithIgnoreCase(name, args[1]))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>(); // 그 외의 경우에는 자동완성 없음
    }

    /**
     * 플레이어가 자신의 섬에 있을 때만 사용 가능한 명령어들을 처리합니다.
     */
    private void handleLoadedIslandCommands(Player player, Island island, String subCommand, String[] args) {
        switch (subCommand) {
            case "업그레이드":
            case "upgrade":
                new IslandUpgradeGUI(island).open(player);
                break;
            case "홈":
            case "sethome":
                if (!player.getWorld().equals(island.getWorld())) {
                    player.sendMessage("§c섬 월드 안에서만 홈을 설정할 수 있습니다.");
                    return;
                }
                island.getData().setSpawnLocation(player.getLocation());
                skyblockManager.saveIslandData(island.getData());
                player.sendMessage("§a현재 위치를 섬의 새로운 스폰 지점으로 설정했습니다.");
                break;
            case "창고":
            case "보관함":
            case "storage":
                Inventory storage = island.getStorage();
                if (storage != null) {
                    player.openInventory(storage);
                } else {
                    player.sendMessage("§c오류: 보관함을 불러올 수 없습니다.");
                }
                break;
        }
    }

    /**
     * 월드 로드 여부와 상관없이 작동하는 데이터 기반 명령어들을 처리합니다.
     */
    private void handleDataCommands(Player player, String subCommand, String[] args) {
        switch (subCommand) {
            case "생성":
            case "create":
                skyblockManager.createIsland(player);
                break;
            case "정보":
            case "info":
                IslandData infoData = skyblockManager.getIslandData(player);
                if (infoData == null) {
                    player.sendMessage("§c소속된 섬이 없습니다.");
                } else {
                    player.sendMessage("§6--- 섬 정보 (ID: " + infoData.getId() + ") ---");
                    String ownerName = Bukkit.getOfflinePlayer(infoData.getOwner()).getName();
                    player.sendMessage("§e소유자: §f" + ownerName);
                    String members = infoData.getMembers().stream()
                            .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                            .filter(name -> name != null)
                            .collect(Collectors.joining(", "));
                    player.sendMessage("§e멤버: §f" + (infoData.getMembers().size() <= 1 ? "없음" : members));
                }
                break;
            case "초대":
            case "invite":
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /섬 초대 <플레이어>");
                    return;
                }
                Player targetToInvite = Bukkit.getPlayer(args[1]);
                if (targetToInvite == null) {
                    player.sendMessage("§c'" + args[1] + "' 플레이어는 온라인 상태가 아닙니다.");
                    return;
                }
                skyblockManager.inviteToIsland(player, targetToInvite);
                break;
            case "수락":
            case "accept":
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /섬 수락 <플레이어>");
                    return;
                }
                Player inviter = Bukkit.getPlayer(args[1]);
                if (inviter == null) {
                    player.sendMessage("§c'" + args[1] + "' 플레이어는 온라인 상태가 아닙니다.");
                    return;
                }
                skyblockManager.acceptInvite(player, inviter);
                break;
            case "추방":
            case "kick":
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /섬 추방 <플레이어>");
                    return;
                }
                Player targetToKick = Bukkit.getPlayer(args[1]);
                if (targetToKick == null) {
                    player.sendMessage("§c'" + args[1] + "' 플레이어는 온라인 상태가 아닙니다.");
                    return;
                }
                skyblockManager.kickFromIsland(player, targetToKick);
                break;
            case "나가기":
            case "leave":
                skyblockManager.leaveIsland(player);
                break;
            default:
                player.sendMessage("§c알 수 없는 명령어입니다. /섬 [생성|정보|초대|수락|추방|나가기|업그레이드|창고|홈]");
                break;
        }
    }
}

