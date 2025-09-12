package Perdume.rpg.gamemode.field.command;

import Perdume.rpg.Rpg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FieldAdminCommand implements CommandExecutor, TabCompleter {

    private final Rpg plugin;

    public FieldAdminCommand(Rpg plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { /* ... */ return true; }
        if (!player.hasPermission("rpg.admin.field")) { /* ... */ return true; }

        if (args.length < 2) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String templateName = args[1];

        switch (subCommand) {
            case "enter":
            case "입장":
                // [핵심] 이제 String을 받는 관리자용 enterField 메소드를 호출합니다.
                player.sendMessage("§e관리자 권한으로 사냥터 '" + templateName + "'에 입장합니다...");
                plugin.getFieldManager().enterField(player, templateName);
                break;

            case "edit":
            case "편집":
                player.performCommand("wa edit field " + templateName);
                break;

            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("rpg.admin.field")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        // 첫 번째 인자 (서브 명령어)
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("입장", "편집"), completions);
        }
        // 두 번째 인자 (사냥터 템플릿 이름)
        else if (args.length == 2) {
            File templateDir = new File(plugin.getDataFolder(), "worlds/field");
            if (templateDir.exists() && templateDir.isDirectory()) {
                String[] files = templateDir.list();
                if (files != null) {
                    StringUtil.copyPartialMatches(args[1], Arrays.asList(files), completions);
                }
            }
        }
        return completions;
    }


    private void sendHelp(Player player) {
        player.sendMessage("§6--- 사냥터 관리 명령어 (/fa) ---");
        player.sendMessage("§e/fa 입장 <사냥터이름> §7- 해당 사냥터의 개인용 인스턴스에 입장합니다.");
        player.sendMessage("§e/fa 편집 <사냥터이름> §7- 해당 사냥터 템플릿을 수정합니다. (/wa edit field 와 동일)");
    }
}