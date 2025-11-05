package Perdume.rpg.core.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import static org.bukkit.Bukkit.getServer;

/**
 * Vault API와 연동하여 서버의 모든 경제 관련 기능을 처리하는 관리자 클래스입니다.
 */
public class EconomyManager {

    private Economy economy = null;

    public boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * 플레이어에게 지정된 양의 돈을 지급합니다.
     * @param player 돈을 받을 플레이어
     * @param amount 지급할 금액
     */
    public void depositPlayer(Player player, double amount) {
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }
}
