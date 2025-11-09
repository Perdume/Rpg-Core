package Perdume.rpg.core.item.gui;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * [신규] 4단계: 커스텀 인챈트 (큐브/잠재능력) GUI
 * (마법부여대 클릭 시 열림)
 */
public class PotentialGUI implements InventoryHolder {

    public static final String GUI_TITLE = "§8[아이템 잠재능력]";

    // GUI 슬롯 정의
    public static final int ITEM_SLOT = 20;
    public static final int CUBE_SLOT = 24;
    public static final int START_SLOT = 31;

    private final Inventory gui;
    private final Rpg plugin;

    private final ItemStack BORDER_ITEM = new ItemFactory(Material.BLACK_STAINED_GLASS_PANE)
            .setDisplayName("§r")
            .build();
    
    public static final ItemStack EMPTY_ITEM_SLOT = new ItemFactory(Material.RED_STAINED_GLASS_PANE)
            .setDisplayName("§c[ 아이템 슬롯 ]")
            .addLore("§7잠재능력을 설정할 장비를 여기에 올려주세요.")
            .build();
    
    public static final ItemStack EMPTY_CUBE_SLOT = new ItemFactory(Material.RED_STAINED_GLASS_PANE)
            .setDisplayName("§c[ 큐브 슬롯 ]")
            .addLore("§7사용할 큐브를 여기에 올려주세요.")
            .build();
            
    public static final ItemStack START_BUTTON_INACTIVE = new ItemFactory(Material.BARRIER)
            .setDisplayName("§c[ 옵션 재설정 불가 ]")
            .addLore("§7아이템과 큐브를 올바르게 올려주세요.")
            .build();

    public PotentialGUI(Rpg plugin) {
        this.plugin = plugin;
        this.gui = Bukkit.createInventory(this, 45, GUI_TITLE);
        initializeGUI();
    }

    private void initializeGUI() {
        // 1. 테두리 채우기
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, BORDER_ITEM);
        }

        // 2. 기능 슬롯 비우기
        gui.setItem(ITEM_SLOT, EMPTY_ITEM_SLOT);
        gui.setItem(CUBE_SLOT, EMPTY_CUBE_SLOT);
        gui.setItem(START_SLOT, START_BUTTON_INACTIVE);

        // 3. (정보 슬롯 - 예: 현재 아이템 등급/옵션 표시)
        // gui.setItem(13, ...); 
    }

    public void open(Player player) {
        player.openInventory(gui);
    }

    @Override
    public Inventory getInventory() {
        return this.gui;
    }
}