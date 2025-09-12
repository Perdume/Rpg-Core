package Perdume.rpg.world.command;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.util.TeleportUtil;
import Perdume.rpg.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class WorldAdminCommand implements CommandExecutor, TabCompleter {

    private final Rpg plugin;
    public static final Map<UUID, EditSession> editingPlayers = new HashMap<>();
    private int nextEditId = 1;

    // [수정] EditSession에 월드 타입을 저장
    public record EditSession(String worldName, String templateName, String type, Location originalLocation) {}

    public WorldAdminCommand(Rpg plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rpg.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        Player player = (sender instanceof Player) ? (Player) sender : null;

        switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list" -> handleList(sender, args);
            case "upgrade" -> handleUpgrade(sender, args);
            case "edit", "tp" -> {
                if (player == null) { sender.sendMessage("§c이 명령어는 플레이어만 사용 가능합니다."); return true; }
                handleTeleportOrEdit(player, args, subCommand.equals("edit"));
            }
            case "save", "leave" -> {
                if (player == null) { sender.sendMessage("§c이 명령어는 플레이어만 사용 가능합니다."); return true; }
                if (subCommand.equals("save")) handleSave(player, (success) -> {});
                else handleLeave(player);
            }
            default -> sendHelp(sender);
        }
        return true;
    }
    /**
     * [핵심] 템플릿 월드를 삭제합니다.
     */
    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /wa delete <type> <템플릿이름>");
            sender.sendMessage("§e(실수로 인한 삭제를 방지하기 위해, 템플릿 폴더를 직접 삭제해주세요.)");
            return;
        }
        // 안전을 위해, 이 명령어는 경고 메시지만 보여주고 실제 파일 삭제는 막아두는 것을 추천합니다.
        // 만약 정말로 명령어로 삭제 기능을 구현하고 싶다면, 아래 주석을 해제하세요.
        /*
        if (args.length < 4 || !args[3].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§c정말로 삭제하시려면 /wa delete " + args[1] + " " + args[2] + " confirm 을 입력하세요.");
            return;
        }
        String type = args[1].toLowerCase();
        String templateName = args[2];

        File templateFolder = new File(plugin.getDataFolder(), "worlds/" + type + "/" + templateName);
        if (!templateFolder.exists()) {
            sender.sendMessage("§c'" + templateName + "' 템플릿을 찾을 수 없습니다.");
            return;
        }

        sender.sendMessage("§e템플릿 월드 '" + templateName + "' 삭제를 시작합니다...");
        WorldManager.deleteWorldFolder(templateFolder);
        sender.sendMessage("§a삭제 작업이 완료되었습니다.");
        */
    }

    /**
     * [핵심] 타입별 템플릿 월드의 목록을 보여줍니다.
     */
    private void handleList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c사용법: /wa list <type>");
            return;
        }
        String type = args[1].toLowerCase();
        File templateDir = new File(plugin.getDataFolder(), "worlds/" + type);

        if (!templateDir.exists() || !templateDir.isDirectory()) {
            sender.sendMessage("§c'" + type + "' 타입의 템플릿 폴더를 찾을 수 없습니다.");
            return;
        }

        String[] files = templateDir.list();
        if (files == null || files.length == 0) {
            sender.sendMessage("§e'" + type + "' 타입으로 등록된 템플릿 월드가 없습니다.");
            return;
        }

        sender.sendMessage("§6--- [" + type.toUpperCase() + "] 템플릿 월드 목록 ---");
        for (String fileName : files) {
            sender.sendMessage("§a- " + fileName);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("rpg.admin")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        List<String> types = List.of("raid", "island", "field");

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "delete", "list", "edit", "tp", "save", "leave", "upgrade"), completions);
        } else if (args.length == 2) {
            if (!args[0].equalsIgnoreCase("save") && !args[0].equalsIgnoreCase("leave")) {
                StringUtil.copyPartialMatches(args[1], types, completions);
            }
        } else if (args.length == 3) {
            if (Arrays.asList("delete", "edit", "tp", "upgrade").contains(args[0].toLowerCase()) && types.contains(args[1].toLowerCase())) {
                File templateDir = new File(plugin.getDataFolder(), "worlds/" + args[1].toLowerCase());
                if (templateDir.exists() && templateDir.isDirectory()) {
                    String[] files = templateDir.list();
                    if (files != null) {
                        StringUtil.copyPartialMatches(args[2], Arrays.asList(files), completions);
                    }
                }
            }
        }
        return completions;
    }

    // --- 각 서브 명령어 처리 메소드 ---

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("§c사용법: /wa create <type> <월드이름>"); return; }
        String type = args[1].toLowerCase();
        String worldName = args[2];

        sender.sendMessage("§e'" + type + "' 타입의 공허 월드 '" + worldName + "' 생성을 시작합니다...");
        if (WorldManager.createVoidTemplate(worldName, type)) {
            sender.sendMessage("§a성공적으로 생성되었습니다! /wa edit " + type + " " + worldName + " 명령어로 수정하세요.");
        } else {
            sender.sendMessage("§c월드 생성에 실패했습니다. 이미 존재하거나 이름이 잘못되었을 수 있습니다.");
        }
    }

    // ... (handleDelete, handleList도 type 인자를 받도록 수정 필요)

    private void handleTeleportOrEdit(Player player, String[] args, boolean isEditMode) {
        if (args.length < 3) { player.sendMessage("§c사용법: /wa " + (isEditMode ? "edit" : "tp") + " <type> <템플릿_맵이름>"); return; }
        String type = args[1].toLowerCase();
        String templateName = args[2];

        if (isEditMode) {
            if (editingPlayers.containsKey(player.getUniqueId())) {
                player.sendMessage("§c이미 수정 중인 맵이 있습니다. 먼저 종료해주세요.");
                return;
            }
            // [핵심] 월드 타입에 따라 임시 월드 이름 접두사 변경
            String prefix = type.substring(0, 1).toUpperCase() + type.substring(1);
            String editWorldName = prefix + "--EDIT--" + Integer.hashCode(nextEditId++);

            player.sendMessage("§e맵 '" + templateName + "'의 수정용 복사본(" + editWorldName + ")을 생성합니다...");
            WorldManager.copyAndLoadWorld(editWorldName, templateName, type, (newWorld) -> {
                if (newWorld != null) {
                    editingPlayers.put(player.getUniqueId(), new EditSession(editWorldName, templateName, type, player.getLocation()));
                    player.teleport(new Location(newWorld, 0.5, 65, 0.5));
                    player.setGameMode(GameMode.CREATIVE);
                    player.sendMessage("§a맵 수정 모드로 진입했습니다. 끝나면 /wa save 또는 /wa leave를 사용해주세요.");
                } else {
                    player.sendMessage("§c맵 생성에 실패했습니다. 템플릿 맵 이름이 정확한지 확인해주세요.");
                }
            });
        } else { // 단순 텔레포트 (템플릿 월드를 임시로 로드해서 보여줌)
            String tempTpWorldName = "Temp--TP--" + templateName;
            player.sendMessage("§e템플릿 월드 '" + templateName + "'을(를) 임시로 불러옵니다...");
            WorldManager.copyAndLoadWorld(tempTpWorldName, templateName, type, (world) -> {
                if (world != null) {
                    player.teleport(new Location(world, 0.5, 65, 0.5));
                    player.sendMessage("§a월드 '" + templateName + "'으로 이동했습니다. 1분 후 자동으로 언로드됩니다.");
                    // 1분 후 자동으로 언로드
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (world.getPlayers().isEmpty()) {
                                WorldManager.unloadAnddeleteWorld(tempTpWorldName);
                            }
                        }
                    }.runTaskLater(plugin, 1200L); // 60초 * 20틱
                } else {
                    player.sendMessage("§c월드를 불러오는 데 실패했습니다.");
                }
            });
        }
    }

    /**
     * [최종 완성본] 현재 수정 중인 맵의 변경사항을 원본 템플릿에 저장하고, 플레이어를 원래 위치로 귀환시킵니다.
     * 다른 클래스(자동 저장 리스너 등)에서도 호출할 수 있도록 public으로 선언합니다.
     * @param player 대상 플레이어
     * @param afterSaveAction 저장이 완료된 후 추가적으로 실행할 작업 (콜백)
     */
    public void handleSave(Player player, Consumer<Boolean> afterSaveAction) {
        // 1. 플레이어의 수정 세션 정보를 가져옵니다.
        EditSession session = editingPlayers.get(player.getUniqueId());
        if (session == null) {
            if (player != null && player.isOnline()) {
                player.sendMessage("§c수정 중인 맵이 없습니다.");
            }
            afterSaveAction.accept(false);
            return;
        }

        // 2. 실제 수정된 월드 객체를 가져옵니다.
        World editWorld = Bukkit.getWorld(session.worldName());
        if (editWorld == null) {
            if (player != null && player.isOnline()) {
                player.sendMessage("§c오류: 수정 중인 월드를 찾을 수 없습니다! 세션이 만료되었을 수 있습니다.");
            }
            editingPlayers.remove(player.getUniqueId());
            afterSaveAction.accept(false);
            return;
        }

        if (player != null && player.isOnline()) {
            player.sendMessage("§e변경사항을 원본 템플릿 '" + session.templateName() + "'에 저장하고 원래 위치로 돌아갑니다...");
        }

        // 3. [핵심] 월드 작업을 하기 전에, 플레이어를 먼저 안전하게 귀환시킵니다.
        TeleportUtil.returnPlayerToSafety(player, session.originalLocation());
        editingPlayers.remove(player.getUniqueId());

        // 4. [핵심] 서버가 플레이어의 이동을 처리할 충분한 시간을 줍니다.
        new BukkitRunnable() {
            @Override
            public void run() {
                // 5. 월드에 플레이어가 아무도 없는 것을 '확인'한 후에야 저장/언로드 작업을 시작합니다.
                if (editWorld.getPlayers().isEmpty()) {
                    WorldManager.saveAndOverwriteTemplate(editWorld, session.templateName(), session.type(), (success) -> {
                        if (player != null && player.isOnline()) {
                            if (success) {
                                player.sendMessage("§a성공적으로 저장되었습니다!");
                            } else {
                                player.sendMessage("§c저장에 실패했습니다. 서버 콘솔을 확인해주세요.");
                            }
                        }
                        afterSaveAction.accept(success);
                    });
                } else {
                    // 만약 5틱 후에도 플레이어가 남아있다면, 에러를 남기고 작업을 중단
                    Rpg.log.severe("맵 저장 실패: 월드 '" + session.worldName() + "'에 아직 플레이어가 남아있습니다.");
                    if (player != null && player.isOnline()) {
                        player.sendMessage("§c오류가 발생하여 맵을 저장하지 못했습니다.");
                    }
                    afterSaveAction.accept(false);
                }
            }
        }.runTaskLater(plugin, 5L); // 5틱 (약 0.25초)의 여유 시간
    }

    private void handleLeave(Player player) {
        EditSession session = editingPlayers.remove(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§c수정 중인 맵이 없습니다.");
            return;
        }

        player.sendMessage("§e수정 사항을 저장하지 않고 맵을 떠납니다...");

        TeleportUtil.returnPlayerToSafety(player, session.originalLocation());

        new BukkitRunnable() {
            @Override
            public void run() {
                WorldManager.unloadAnddeleteWorld(session.worldName());
            }
        }.runTaskLater(plugin, 5L);
    }

    /**
     * [신규] 템플릿 월드를 현재 서버 버전에 맞게 업그레이드합니다.
     */
    private void handleUpgrade(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /wa upgrade <type> <템플릿이름>");
            return;
        }

        String type = args[1].toLowerCase();
        String templateName = args[2];
        String tempWorldName = "TEMP_UPGRADE_" + templateName;

        File templateFolder = new File(plugin.getDataFolder(), "worlds/" + type + "/" + templateName);
        File tempWorldFolder = new File(Bukkit.getWorldContainer(), tempWorldName);

        if (!templateFolder.exists()) {
            sender.sendMessage("§c원본 템플릿 월드 '" + templateName + "'을(를) 찾을 수 없습니다.");
            return;
        }

        sender.sendMessage("§e월드 업그레이드를 준비합니다... (원본 템플릿 복사 중)");

        // 비동기 스레드에서 파일 작업을 수행
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // 1. 원본 템플릿을 서버 루트에 임시로 복사
                    WorldManager.copyWorldFolder(templateFolder, tempWorldFolder);

                    // 2. 메인 스레드에서 서버의 업그레이드 명령어를 실행
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage("§e월드 데이터 변환을 시작합니다. 서버가 잠시 멈출 수 있습니다. 콘솔을 확인해주세요.");
                            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                            Bukkit.dispatchCommand(console, "upgrade " + tempWorldName + " --now");

                            // TODO: 업그레이드 완료를 감지하는 더 정교한 방법이 필요.
                            // 현재는 명령어가 끝날 때까지 기다린다고 가정하고, 10초 후 후처리 작업을 예약.
                            sender.sendMessage("§e10초 후 후속 작업을 진행합니다...");
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    sender.sendMessage("§e업그레이드된 월드를 원본 위치로 덮어씌웁니다...");
                                    // 3. 업그레이드된 임시 폴더를 다시 원본 위치로 덮어씌움
                                    WorldManager.deleteWorldFolder(templateFolder);
                                    tempWorldFolder.renameTo(templateFolder);
                                    sender.sendMessage("§a월드 '" + templateName + "' 업그레이드가 완료되었습니다!");
                                }
                            }.runTaskLater(plugin, 200L); // 10초
                        }
                    }.runTask(plugin);
                } catch (Exception e) {
                    sender.sendMessage("§c월드 업그레이드 중 오류가 발생했습니다.");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6--- RPG 월드 관리 명령어 (/wa) ---");
        sender.sendMessage("§e/wa list <type> §7- 타입별 템플릿 목록 (raid, island, field)");
        sender.sendMessage("§e/wa create <type> <name> §7- 새로운 공허 템플릿 생성");
        sender.sendMessage("§e/wa delete <type> <name> §7- 템플릿 삭제 (폴더 직접 삭제 권장)");
        sender.sendMessage("§e/wa edit <type> <name> §7- 월드 수정 모드 진입");
        sender.sendMessage("§e/wa tp <type> <name> §7- 템플릿 월드로 임시 이동");
        sender.sendMessage("§e/wa upgrade <type> <name> §7- 템플릿을 현재 서버 버전으로 업그레이드");
        sender.sendMessage("§e/wa save §7- 수정 사항을 저장하고 나가기");
        sender.sendMessage("§e/wa leave §7- 수정 사항을 버리고 나가기");
    }
}