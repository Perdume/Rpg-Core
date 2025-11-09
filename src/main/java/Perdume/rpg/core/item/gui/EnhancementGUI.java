package Perdume.rpg.core.item.gui;

import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 아이템 강화 GUI (모루 클릭 시 열림)
 */
public class EnhancementGUI implements InventoryHolder {

    public static final String GUI_TITLE = "§8[아이템 강화]";

    // GUI 슬롯 정의
    public static final int ITEM_SLOT = 20;
    public static final int CATALYST_SLOT = 24;
    public static final int START_SLOT = 31;

    private final Inventory gui;

    private final ItemStack BORDER_ITEM = new ItemFactory(Material.GRAY_STAINED_GLASS_PANE)
            .setDisplayName("§r")
            .build();

    // [핵심 수정] private final -> public static final로 변경
    public static final ItemStack EMPTY_ITEM_SLOT = new ItemFactory(Material.RED_STAINED_GLASS_PANE)
            .setDisplayName("§c[ 아이템 슬롯 ]")
            .addLore("§7강화할 장비를 여기에 올려주세요.")
            .build();
    // [핵심 수정] private final -> public static final로 변경
    public static final ItemStack EMPTY_CATALYST_SLOT = new ItemFactory(Material.RED_STAINED_GLASS_PANE)
            .setDisplayName("§c[ 재료 슬롯 ]")
            .addLore("§7강화 재료(주문서 등)를 여기에 올려주세요.")
            .build();
    public static final ItemStack START_BUTTON_INACTIVE = new ItemFactory(Material.BARRIER)
            .setDisplayName("§c[ 강화 불가 ]")
            .addLore("§7아이템과 재료를 올바르게 올려주세요.")
            .build();

    public EnhancementGUI() {
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
        gui.setItem(CATALYST_SLOT, EMPTY_CATALYST_SLOT);
        gui.setItem(START_SLOT, START_BUTTON_INACTIVE);

        // 3. (정보 슬롯 - 나중에 추가)
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