package Perdume.rpg.core.item.gui;

import Perdume.rpg.core.item.ItemManager;
import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * ItemManager에 등록된 모든 커스텀 아이템을 페이지별로 보여주는 GUI입니다.
 */
public class ItemAdminGUI {

    private final ItemManager itemManager;
    private final Inventory inventory;
    private final int page;
    private final int maxPage;

    // GUI의 크기와 아이템이 표시될 슬롯의 개수
    private static final int GUI_SIZE = 54;
    private static final int ITEM_SLOTS = 45;

    public ItemAdminGUI(ItemManager itemManager, int page) {
        this.itemManager = itemManager;
        this.page = page;
        List<ItemStack> allItems = itemManager.getAllItems();
        this.maxPage = (int) Math.ceil((double) allItems.size() / ITEM_SLOTS);
        this.inventory = Bukkit.createInventory(null, GUI_SIZE, "§8[커스텀 아이템 목록] (" + page + "/" + maxPage + ")");

        initializeItems();
    }

    private void initializeItems() {
        List<ItemStack> allItems = itemManager.getAllItems();
        int startIndex = (page - 1) * ITEM_SLOTS;

        // 아이템 채우기
        for (int i = 0; i < ITEM_SLOTS; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < allItems.size()) {
                inventory.setItem(i, allItems.get(itemIndex));
            }
        }

        // 페이지 이동 버튼
        if (page > 1) {
            inventory.setItem(45, new ItemFactory(Material.ARROW).setDisplayName("§e이전 페이지").build());
        }
        if (page < maxPage) {
            inventory.setItem(53, new ItemFactory(Material.ARROW).setDisplayName("§e다음 페이지").build());
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }
}
