package Perdume.rpg.command;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.player.gui.StatsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * '/스탯' 또는 '/내정보' 명령어를 처리하여
 * 플레이어에게 캐릭터 정보 GUI를 열어주는 클래스입니다.
 */
public class StatsCommand implements CommandExecutor {

    private final Rpg plugin;

    public StatsCommand(Rpg plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        Player player = (Player) sender;

        // 새로운 StatsGUI 객체를 생성하고 플레이어에게 열어줍니다.
        // GUI를 생성하는 데 필요한 모든 정보는 StatsGUI 클래스 내부에서 처리합니다.
        new StatsGUI(plugin, player).open();

        return true;
    }
}
