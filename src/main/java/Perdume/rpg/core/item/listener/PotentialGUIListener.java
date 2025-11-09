package Perdume.rpg.core.item.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.ItemManager;
import Perdume.rpg.core.item.PotentialManager;
import Perdume.rpg.core.item.gui.PotentialGUI;
import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * [신규] 4단계: PotentialGUI (큐브/잠재능력)의 클릭 이벤트를 처리합니다.
 * (EnhancementGUIListener 로직 기반)
 */
public class PotentialGUIListener implements Listener {

    private final Rpg plugin;
    private final ItemManager itemManager;
    private final PotentialManager potentialManager;

    public PotentialGUIListener(Rpg plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        this.potentialManager = plugin.getPotentialManager();
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(PotentialGUI.GUI_TITLE)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory gui = event.getInventory();
        int slot = event.getRawSlot();

        // 1. 큐브 사용 시작 버튼 클릭
        if (slot == PotentialGUI.START_SLOT) {
            event.setCancelled(true);
            ItemStack button = gui.getItem(slot);
            if (button == null || button.isSimilar(PotentialGUI.START_BUTTON_INACTIVE)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } else {
                // 아이템 슬롯에서 아이템 가져오기
                ItemStack item = gui.getItem(PotentialGUI.ITEM_SLOT);
                ItemStack cube = gui.getItem(PotentialGUI.CUBE_SLOT);

                if (item != null && cube != null) {
                    // 큐브 사용 시도
                    boolean success = potentialManager.applyCube(item, cube);

                    if (success) {
                        // 재료 소모
                        cube.setAmount(cube.getAmount() - 1);
                        gui.setItem(PotentialGUI.CUBE_SLOT, cube.getAmount() > 0 ? cube : null);
                        
                        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
                        player.sendMessage("§b[잠재능력] §f아이템의 옵션이 재설정되었습니다!");
                        gui.setItem(PotentialGUI.ITEM_SLOT, item); // 변경된 아이템 (NBT, Lore 갱신됨)
                    } else {
                         player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                         player.sendMessage("§c[잠재능력] §f옵션 재설정에 실패했습니다.");
                    }
                }
            }

        // 2. GUI의 기능 슬롯 (아이템, 큐브) 클릭
        } else if (slot == PotentialGUI.ITEM_SLOT || slot == PotentialGUI.CUBE_SLOT) {

            ItemStack currentItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();

            boolean isPlaceholder = currentItem != null && (currentItem.isSimilar(PotentialGUI.EMPTY_ITEM_SLOT) ||
                    currentItem.isSimilar(PotentialGUI.EMPTY_CUBE_SLOT));
            boolean cursorEmpty = cursorItem == null || cursorItem.getType().isAir();

            if (isPlaceholder && !cursorEmpty) {
                event.setCancelled(true);
                gui.setItem(slot, cursorItem.clone());
                player.setItemOnCursor(null);
            } else if (isPlaceholder && cursorEmpty) {
                event.setCancelled(true);
            } else {
                event.setCancelled(false);
            }

        // 3. GUI의 빈 공간 (테두리) 클릭 방지
        } else if (slot < gui.getSize()) {
            event.setCancelled(true);

        // 4. 플레이어 인벤토리 클릭 (shift-click 등)
        } else {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType().isAir()) {
                     plugin.getServer().getScheduler().runTask(plugin, () -> updateButtonStatus(gui));
                    return;
                }

                // (임시) 큐브인지 아이템인지 NBT를 읽어서 판단해야 함
                // (EnhancementManager.isEnhanceable/isCatalyst와 유사한 로직 필요)
                // TODO: PotentialManager에 isPotentialItem() / isCube() 메소드 추가
                
                // (임시 로직: 큐브 슬롯이 비어있고, 아이템 ID에 "cube"가 포함되면 큐브로 간주)
                String itemId = itemManager.getItemId(clickedItem);
                if (itemId != null && itemId.contains("cube")) {
                     ItemStack cubeSlot = gui.getItem(PotentialGUI.CUBE_SLOT);
                     if (cubeSlot != null && cubeSlot.isSimilar(PotentialGUI.EMPTY_CUBE_SLOT)) {
                        gui.setItem(PotentialGUI.CUBE_SLOT, clickedItem.clone());
                        event.setCurrentItem(null);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                     }
                } else {
                    // 아이템으로 간주
                    ItemStack itemSlot = gui.getItem(PotentialGUI.ITEM_SLOT);
                    if (itemSlot != null && itemSlot.isSimilar(PotentialGUI.EMPTY_ITEM_SLOT)) {
                        gui.setItem(PotentialGUI.ITEM_SLOT, clickedItem.clone());
                        event.setCurrentItem(null);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    }
                }

            } else {
                event.setCancelled(false);
            }
        }

        // 버튼 업데이트
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            updateButtonStatus(gui);
        });
    }

    /**
     * GUI가 닫힐 때, 슬롯에 남아있는 아이템을 플레이어에게 돌려줍니다.
     */
    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(PotentialGUI.GUI_TITLE)) {
            return;
        }
        Inventory gui = event.getInventory();
        Player player = (Player) event.getPlayer();

        ItemStack item = gui.getItem(PotentialGUI.ITEM_SLOT);
        ItemStack cube = gui.getItem(PotentialGUI.CUBE_SLOT);

        if (item != null && !item.getType().isAir() && !item.isSimilar(PotentialGUI.EMPTY_ITEM_SLOT)) {
            player.getInventory().addItem(item);
        }
        if (cube != null && !cube.getType().isAir() && !cube.isSimilar(PotentialGUI.EMPTY_CUBE_SLOT)) {
             player.getInventory().addItem(cube);
        }
    }


    /**
     * (Helper) 아이템/큐브 슬롯의 상태를 확인하고 버튼을 활성화/비활성화합니다.
     */
    private void updateButtonStatus(Inventory gui) {
        ItemStack item = gui.getItem(PotentialGUI.ITEM_SLOT);
        ItemStack cube = gui.getItem(PotentialGUI.CUBE_SLOT);

        // TODO: PotentialManager.isPotentialItem(item) / isCube(cube)로 교체
        boolean itemOk = item != null && !item.isSimilar(PotentialGUI.EMPTY_ITEM_SLOT);
        boolean cubeOk = cube != null && !cube.isSimilar(PotentialGUI.EMPTY_CUBE_SLOT);

        if (itemOk && cubeOk) {
            // [활성화]
            ItemStack activeButton = new ItemFactory(Material.ENCHANTING_TABLE)
                    .setDisplayName("§b[ 옵션 재설정 ]")
                    .addLore("§7클릭하여 큐브를 사용합니다.")
                    .build();
            gui.setItem(PotentialGUI.START_SLOT, activeButton);
        } else {
            // [비활성화]
            gui.setItem(PotentialGUI.START_SLOT, PotentialGUI.START_BUTTON_INACTIVE);
        }
    }
}