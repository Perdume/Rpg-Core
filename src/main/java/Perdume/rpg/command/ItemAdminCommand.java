package Perdume.rpg.command;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.gui.ItemAdminGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 관리자가 모든 커스텀 아이템을 얻을 수 있는 GUI를 여는 명령어(/rpgitem)를 처리합니다.
 */
public class ItemAdminCommand implements CommandExecutor {

    private final Rpg plugin;

    public ItemAdminCommand(Rpg plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        // ItemAdminGUI를 열어줍니다. (페이지 기능이 있으므로 1페이지부터 시작)
        new ItemAdminGUI(plugin.getItemManager(), 1).open(player);
        return true;
    }
}
