package Perdume.rpg.core.item.crafting.gui;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 커스텀 제작대 GUI (3x3 조합 + 결과물 + 레시피북)
 * 54칸 상자 GUI 기반
 * [최종 수정] 조합 불가 시 표시할 방벽 아이템 추가
 */
public class CustomCraftGUI implements InventoryHolder {

    private final Rpg plugin;
    private final Inventory gui;

    // GUI 슬롯 정의
    private final List<Integer> craftingSlots = List.of(
            10, 11, 12, 19, 20, 21, 28, 29, 30
    );
    private final int RESULT_SLOT = 24;
    private final int RECIPE_BOOK_SLOT = 8;
    private final ItemStack BORDER_ITEM = new ItemFactory(Material.GRAY_STAINED_GLASS_PANE)
            .setDisplayName("§r")
            .build();
    private final ItemStack RECIPE_BOOK_ITEM = new ItemFactory(Material.BOOK)
            .setDisplayName("§a레시피북")
            .addLore("§7클릭하여 조합법 목록을 엽니다.")
            .build();

    // [신규 추가] 조합법이 없을 때 표시할 아이템 (방벽)
    private final ItemStack NO_RECIPE_ITEM = new ItemFactory(Material.BARRIER)
            .setDisplayName("§c조합법 없음")
            .addLore("§7재료가 올바르지 않습니다.")
            .build();

    public CustomCraftGUI(Rpg plugin) {
        this.plugin = plugin;
        this.gui = Bukkit.createInventory(this, 54, "커스텀 제작");
        initializeGUI();
    }

    private void initializeGUI() {
        // 1. 테두리 채우기
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, BORDER_ITEM);
        }

        // 2. 3x3 제작 슬롯 비우기
        for (int slot : craftingSlots) {
            gui.setItem(slot, null);
        }

        // 3. 결과 슬롯 초기화 (방벽으로)
        gui.setItem(RESULT_SLOT, NO_RECIPE_ITEM.clone());

        // 4. 레시피북 아이콘 배치
        gui.setItem(RECIPE_BOOK_SLOT, RECIPE_BOOK_ITEM);
    }

    public void open(Player player) {
        player.openInventory(gui);
    }

    @Override
    public Inventory getInventory() {
        return this.gui;
    }

    /**
     * [신규 추가]
     * CustomCraftGUIListener가 "조합법 없음" 아이템(방벽)을
     * 가져갈 수 있도록 getter 메서드를 제공합니다.
     * @return 방벽 아이템 (복사본)
     */
    public ItemStack getNoRecipeItem() {
        return this.NO_RECIPE_ITEM.clone();
    }
}