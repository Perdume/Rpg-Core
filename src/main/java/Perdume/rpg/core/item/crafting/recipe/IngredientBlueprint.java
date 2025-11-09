package Perdume.rpg.core.item.crafting.recipe;

import Perdume.rpg.core.item.CustomItemUtil; // CustomItemUtil import
// [제거] import org.bukkit.Bukkit;
// [제거] import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
// [제거] import java.util.logging.Logger;

public class IngredientBlueprint {
    private final ItemStack itemStack;
    private int amount; // 재료 수량을 저장할 변수

    // (기존 생성자 - 호환성을 위해 유지)
    public IngredientBlueprint(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.amount = 1;
    }

    // RecipeManager에서 사용할 새 생성자
    public IngredientBlueprint(ItemStack itemStack, int amount) {
        this.itemStack = itemStack;
        this.amount = amount;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getAmount() {
        return amount;
    }

    // [제거] getDebugName() 메서드

    /**
     * [수정됨] 이 재료 명세서가 실제 아이템과 일치하는지 확인합니다.
     * (Shaped/Shapeless 레시피에서 호출)
     *
     * @param actualItem 조합창 슬롯에 있는 실제 아이템
     * @return 일치 여부
     */
    public boolean matches(ItemStack actualItem) {
        // [제거] Logger 및 디버그용 변수

        // 1. 필요한 아이템이 있는데, 실제 슬롯은 비어있는 경우
        if (actualItem == null || actualItem.getType().isAir()) {
            // [제거] Ingredient FAILED 로그
            return false;
        }

        // [제거] 실제 아이템 이름 가져오기 및 [Ingredient matches?] 로그

        // 2. 아이템 종류 비교 (커스텀/바닐라 모두 비교)
        boolean similar = CustomItemUtil.isSimilar(this.itemStack, actualItem);
        if (!similar) {
            // [제거] Ingredient FAILED 로그
            return false;
        }

        // 3. 아이템 수량 비교
        boolean amountOk = actualItem.getAmount() >= this.amount;
        if (!amountOk) {
            // [제거] Ingredient FAILED 로그
            return false;
        }

        // 모든 조건을 통과
        // [제거] Ingredient SUCCESS 로그
        return true;
    }
}