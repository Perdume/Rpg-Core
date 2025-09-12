package Perdume.rpg.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.system.FieldManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class FieldInstanceListener implements Listener {

    private final Rpg plugin;
    private final FieldManager fieldManager;

    public FieldInstanceListener(Rpg plugin) {
        this.plugin = plugin;
        this.fieldManager = plugin.getFieldManager();
    }

    // 플레이어가 월드를 이동했을 때
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // [핵심] 떠나온 월드가 '사냥터'였다면, FieldManager에게 보고합니다.
        World fromWorld = event.getFrom();
        if (fromWorld.getName().startsWith("Field--")) {
            fieldManager.leaveField(event.getPlayer());
        }
    }

    // 플레이어가 접속 종료했을 때
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // [핵심] 마찬가지로, FieldManager에게 보고합니다.
        fieldManager.leaveField(event.getPlayer());
    }
}