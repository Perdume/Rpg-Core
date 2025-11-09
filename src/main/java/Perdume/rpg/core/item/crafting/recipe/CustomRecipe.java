package Perdume.rpg.core.item.crafting.recipe;

import org.bukkit.inventory.ItemStack;

/**
 * Shaped, Shapeless 레시피를 하나로 묶는 인터페이스입니다.
 * [최종 수정] matches의 파라미터를 ItemStack[]로 변경
 */
public interface CustomRecipe {

    /**
     * 조합창의 상태가 이 레시피와 일치하는지 확인합니다.
     * @param matrix 3x3 조합 매트릭스 (ItemStack 배열, 9칸)
     * @return 일치 여부
     */
    boolean matches(ItemStack[] matrix);

    /**
     * 이 레시피의 결과 아이템을 반환합니다.
     * @return 결과 아이템 (복사본)
     */
    ItemStack getResult();
}