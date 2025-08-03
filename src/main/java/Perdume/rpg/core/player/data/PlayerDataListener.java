package Perdume.rpg.core.player.data;

import Perdume.rpg.Rpg;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDataListener implements Listener {
    private final PlayerDataManager playerDataManager;

    public PlayerDataListener(Rpg plugin) {
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    /**
     * 플레이어가 서버에 접속할 때 데이터를 미리 로드합니다.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerDataManager.loadPlayerDataOnJoin(event.getPlayer());
    }

    /**
     * 플레이어가 서버에서 나갈 때 데이터를 파일에 저장합니다.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataManager.savePlayerDataOnQuit(event.getPlayer());
    }
}