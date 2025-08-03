package Perdume.rpg.gamemode.island.gui;

import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IslandMemberGUI {

    public static final String GUI_TITLE = "§8[섬 멤버 관리]";

    public static void open(Player player, Island island) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        // 멤버 목록 표시
        for (UUID memberUuid : island.getMembers()) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(memberUuid);
            ItemStack memberHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) memberHead.getItemMeta();
            
            meta.setOwningPlayer(member);
            meta.setDisplayName("§a" + member.getName());
            
            List<String> lore = new ArrayList<>();
            if (island.getOwner().equals(memberUuid)) {
                lore.add("§6[섬 주인]");
            } else {
                lore.add("§7[멤버]");
                lore.add(" ");
                lore.add("§c클릭하여 추방하기");
            }
            meta.setLore(lore);
            memberHead.setItemMeta(meta);
            gui.addItem(memberHead);
        }

        // 새 멤버 초대 버튼
        ItemStack inviteItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta inviteMeta = inviteItem.getItemMeta();
        inviteMeta.setDisplayName("§b[ 새 멤버 초대 ]");
        inviteMeta.setLore(List.of("§7클릭하여 채팅으로 초대할 플레이어의", "§7이름을 입력하세요."));
        inviteItem.setItemMeta(inviteMeta);
        gui.setItem(49, inviteItem);

        player.openInventory(gui);
    }
}