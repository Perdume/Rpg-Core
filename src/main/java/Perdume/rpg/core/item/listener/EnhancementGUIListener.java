package Perdume.rpg.core.item.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.EnhancementManager;
import Perdume.rpg.core.item.gui.EnhancementGUI;
import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * EnhancementGUI (강화)의 클릭 이벤트를 처리하는 리스너입니다.
 */
public class EnhancementGUIListener implements Listener {

    private final Rpg plugin;
    private final EnhancementManager enhancementManager;

    public EnhancementGUIListener(Rpg plugin) {
        this.plugin = plugin;
        this.enhancementManager = plugin.getEnhancementManager();
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(EnhancementGUI.GUI_TITLE)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory gui = event.getInventory();
        int slot = event.getRawSlot();

        // 1. 강화 시작 버튼 클릭
        if (slot == EnhancementGUI.START_SLOT) {
            event.setCancelled(true);
            ItemStack button = gui.getItem(slot);
            if (button == null || button.isSimilar(EnhancementGUI.START_BUTTON_INACTIVE)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } else {
                // 아이템 슬롯에서 아이템 가져오기
                ItemStack item = gui.getItem(EnhancementGUI.ITEM_SLOT);
                ItemStack catalyst = gui.getItem(EnhancementGUI.CATALYST_SLOT);

                if (item != null && catalyst != null) {
                    // 강화 시도
                    EnhancementManager.EnhanceResult result = enhancementManager.enhanceItem(item);

                    // 재료 소모
                    catalyst.setAmount(catalyst.getAmount() - 1);
                    gui.setItem(EnhancementGUI.CATALYST_SLOT, catalyst.getAmount() > 0 ? catalyst : null);

                    // 결과 처리
                    switch (result) {
                        case SUCCESS:
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                            player.sendMessage("§a[강화] §f강화에 성공했습니다!");
                            gui.setItem(EnhancementGUI.ITEM_SLOT, item); // 변경된 아이템 (NBT, Lore 갱신됨)
                            break;
                        case FAIL:
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                            player.sendMessage("§c[강화] §f강화에 실패했습니다...");
                            break;
                        case DESTROY:
                            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.8f);
                            player.sendMessage("§4[강화] §c아이템이 파괴되었습니다...");
                            gui.setItem(EnhancementGUI.ITEM_SLOT, null); // 아이템 소멸
                            break;
                    }
                }
            }

            // 2. GUI의 기능 슬롯 (아이템, 재료) 클릭
        } else if (slot == EnhancementGUI.ITEM_SLOT || slot == EnhancementGUI.CATALYST_SLOT) {

            ItemStack currentItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();

            boolean isPlaceholder = currentItem != null && (currentItem.isSimilar(EnhancementGUI.EMPTY_ITEM_SLOT) ||
                    currentItem.isSimilar(EnhancementGUI.EMPTY_CATALYST_SLOT));
            boolean cursorEmpty = cursorItem == null || cursorItem.getType().isAir();

            if (isPlaceholder && !cursorEmpty) {
                // [버그 수정 1] 아이템을 플레이스홀더에 '놓을' 때
                event.setCancelled(true);
                gui.setItem(slot, cursorItem.clone());
                player.setItemOnCursor(null);

            } else if (isPlaceholder && cursorEmpty) {
                // [버그 수정 1] 플레이스홀더를 '집으려' 할 때
                event.setCancelled(true);

            } else {
                // 플레이스홀더가 아닌 '실제 아이템'을 집거나,
                // '빈 슬롯'에 아이템을 놓는 행위 등은 허용합니다.
                event.setCancelled(false);
            }

            // 3. GUI의 빈 공간 (테두리) 클릭 방지
        } else if (slot < gui.getSize()) {
            event.setCancelled(true);

            // 4. 플레이어 인벤토리 클릭 (shift-click 등)
        } else {
            // [핵심 수정] Shift-Click 자동 등록 로직
            if (event.isShiftClick()) {
                event.setCancelled(true); // Bukkit의 기본 동작(빈 칸에 채우기)을 막습니다.

                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType().isAir()) {
                    // (runTask for updateButtonStatus will still run)
                    plugin.getServer().getScheduler().runTask(plugin, () -> updateButtonStatus(gui));
                    return;
                }

                // 1. 강화 가능한 아이템인가? (예: 무기, 방어구)
                if (enhancementManager.isEnhanceable(clickedItem)) {
                    ItemStack itemSlot = gui.getItem(EnhancementGUI.ITEM_SLOT);
                    // 아이템 슬롯이 플레이스홀더(빈 칸)일 때만 아이템을 옮깁니다.
                    if (itemSlot != null && itemSlot.isSimilar(EnhancementGUI.EMPTY_ITEM_SLOT)) {
                        gui.setItem(EnhancementGUI.ITEM_SLOT, clickedItem.clone());
                        event.setCurrentItem(null); // 플레이어 인벤토리에서 제거
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }

                    // 2. 강화 재료(촉매제)인가?
                } else if (enhancementManager.isCatalyst(clickedItem)) {
                    ItemStack catalystSlot = gui.getItem(EnhancementGUI.CATALYST_SLOT);
                    // 재료 슬롯이 비어있을 때
                    if (catalystSlot != null && catalystSlot.isSimilar(EnhancementGUI.EMPTY_CATALYST_SLOT)) {
                        gui.setItem(EnhancementGUI.CATALYST_SLOT, clickedItem.clone());
                        event.setCurrentItem(null);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

                        // 재료 슬롯에 이미 같은 재료가 있고, 겹칠 수 있을 때
                    } else if (catalystSlot != null && catalystSlot.isSimilar(clickedItem) &&
                            (catalystSlot.getAmount() < catalystSlot.getMaxStackSize())) {

                        int amountToMove = clickedItem.getAmount();
                        int spaceLeft = catalystSlot.getMaxStackSize() - catalystSlot.getAmount();

                        if (amountToMove <= spaceLeft) {
                            catalystSlot.setAmount(catalystSlot.getAmount() + amountToMove);
                            event.setCurrentItem(null); // 플레이어 인벤에서 모두 제거
                        } else {
                            catalystSlot.setAmount(catalystSlot.getMaxStackSize());
                            clickedItem.setAmount(amountToMove - spaceLeft); // 남은 수량
                        }
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                }

            } else {
                // Shift-Click이 아닌 일반 클릭 (플레이어 인벤토리 내)
                event.setCancelled(false);
            }
        }

        // [수정 없음] 버튼 업데이트는 항상 모든 클릭 이후에 0틱 딜레이로 실행됩니다.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            updateButtonStatus(gui);
        });
    }

    /**
     * [신규 추가] 아이템을 "드래그"로 놓았을 때 갱신
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGuiDrag(InventoryDragEvent event) {
        // 1. 올바른 GUI인지 확인
        if (!event.getView().getTitle().equals(EnhancementGUI.GUI_TITLE)) {
            return;
        }

        Inventory gui = event.getInventory();
        boolean affectsEnhancementSlots = false;

        // 2. 드래그가 강화 슬롯(아이템, 재료)에 영향을 주는지 확인
        for (int slot : event.getRawSlots()) {
            if (slot == EnhancementGUI.ITEM_SLOT || slot == EnhancementGUI.CATALYST_SLOT) {
                affectsEnhancementSlots = true;
                break;
            }
        }

        // 3. 영향을 준다면, 다음 틱에 버튼 상태 갱신
        if (affectsEnhancementSlots) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                updateButtonStatus(gui);
            });
        }
    }

    /**
     * GUI가 닫힐 때, 슬롯에 남아있는 아이템을 플레이어에게 돌려줍니다.
     */
    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(EnhancementGUI.GUI_TITLE)) {
            return;
        }
        Inventory gui = event.getInventory();
        Player player = (Player) event.getPlayer();

        ItemStack item = gui.getItem(EnhancementGUI.ITEM_SLOT);
        ItemStack catalyst = gui.getItem(EnhancementGUI.CATALYST_SLOT);

        // 비어있는 슬롯 표시용 아이템(빨간 유리판)은 반환하지 않도록 체크
        if (item != null && !item.getType().isAir() && !item.isSimilar(EnhancementGUI.EMPTY_ITEM_SLOT)) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        }
        if (catalyst != null && !catalyst.getType().isAir() && !catalyst.isSimilar(EnhancementGUI.EMPTY_CATALYST_SLOT)) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(catalyst);
            leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        }
    }


    /**
     * (Helper) 아이템/재료 슬롯의 상태를 확인하고 강화 버튼을 활성화/비활성화합니다.
     */
    private void updateButtonStatus(Inventory gui) {
        ItemStack item = gui.getItem(EnhancementGUI.ITEM_SLOT);
        ItemStack catalyst = gui.getItem(EnhancementGUI.CATALYST_SLOT);

        // 1. 아이템이 강화 가능한가? (플레이스홀더가 아니고, 강화 가능한 아이템인지)
        boolean itemOk = enhancementManager.isEnhanceable(item);
        // 2. 재료가 올바른가? (플레이스홀더가 아니고, 올바른 재료인지)
        boolean catalystOk = enhancementManager.isCatalyst(catalyst);

        if (itemOk && catalystOk) {
            // [활성화]
            int currentLevel = enhancementManager.getEnhanceLevel(item);

            // [신규] 최대 레벨인지 확인
            if (enhancementManager.isMaxLevel(item, currentLevel)) {
                ItemStack maxLevelButton = new ItemFactory(Material.BARRIER)
                        .setDisplayName("§c[ 최대 레벨 ]")
                        .addLore("§7더 이상 강화할 수 없습니다.")
                        .build();
                gui.setItem(EnhancementGUI.START_SLOT, maxLevelButton);
                return;
            }

            ItemStack activeButton = new ItemFactory(Material.ANVIL)
                    .setDisplayName("§a[ 강화 시작: " + currentLevel + " -> " + (currentLevel + 1) + " ]")
                    .addLore("§7클릭하여 강화를 시도합니다.")
                    .addLore(enhancementManager.getChanceLore(item, currentLevel)) // 확률 Lore 추가
                    .build();
            gui.setItem(EnhancementGUI.START_SLOT, activeButton);
        } else {
            // [비활성화]
            gui.setItem(EnhancementGUI.START_SLOT, EnhancementGUI.START_BUTTON_INACTIVE);
        }
    }
}