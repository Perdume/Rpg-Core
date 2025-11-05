package Perdume.rpg.core.item.crafting.recipe;

import Perdume.rpg.core.item.CustomItemUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * [로직 구현] YML에 정의된 하나의 재료 명세서입니다.
 * 바닐라/커스텀 아이템, 개수를 모두 검사(matches)하는 로직을 포함합니다.
 */
public class IngredientBlueprint {

    public enum Type { VANILLA, CUSTOM }

    private final Type type;
    private final Material material; // VANILLA 타입일 때만 사용
    private final String customId;   // CUSTOM 타입일 때만 사용
    private final int amount;

    /**
     * 바닐라 재료 명세서를 생성합니다.
     */
    public IngredientBlueprint(Material material, int amount) {
        this.type = Type.VANILLA;
        this.material = material;
        this.customId = null;
        this.amount = amount;
    }

    /**
     * 커스텀 재료 명세서를 생성합니다.
     */
    public IngredientBlueprint(String customId, int amount) {
        this.type = Type.CUSTOM;
        this.material = null; // 커스텀 아이템은 ID로만 구분
        this.customId = customId;
        this.amount = amount;
    }

    /**
     * 실제 조합창의 아이템과 이 재료 명세서가 일치하는지 검사합니다.
     * @param itemStack 조합창의 실제 아이템
     * @return 일치하면 true
     */
    public boolean matches(ItemStack itemStack) {
        // 1. 빈 슬롯 검사
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false; // 명세서는 항상 무언가를 요구하는데, 슬롯이 비었으므로 불일치
        }

        // 2. 개수 검사
        if (itemStack.getAmount() != this.amount) {
            return false;
        }

        // 3. 타입 검사 (바닐라 / 커스텀)
        if (this.type == Type.VANILLA) {
            // 바닐라 아이템은 Material 타입만 비교합니다.
            return itemStack.getType() == this.material;
        } else {
            // 커스텀 아이템은 NBT ID를 비교합니다.
            String itemId = CustomItemUtil.getCustomId(itemStack);
            return Objects.equals(this.customId, itemId);
        }
    }
}
