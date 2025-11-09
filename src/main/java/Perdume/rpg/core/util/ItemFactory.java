package Perdume.rpg.core.util;

// [추가] NBT 태그 및 ItemManager 접근에 필요한 import
import Perdume.rpg.core.item.ItemManager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

// [추가] addLore에 필요한 import
import java.util.ArrayList;
import java.util.List;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 빌더 패턴을 사용하여 ItemStack을 보다 편리하게 생성하는 유틸리티 클래스입니다.
 * (사용자님이 제공한 기준 코드)
 */
public class ItemFactory {

    private final ItemStack item;
    private final ItemMeta meta;

    /**
     * [기존] Material로부터 새 아이템을 생성합니다.
     * @param material
     */
    public ItemFactory(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    /**
     * [신규 추가] 기존 ItemStack을 수정합니다.
     * (RecipeBookGUI의 'Provided: ItemStack' 오류를 해결하기 위함)
     * @param item 수정할 ItemStack
     */
    public ItemFactory(ItemStack item) {
        this.item = item;
        this.meta = item.getItemMeta();
    }

    public ItemFactory setDisplayName(String name) {
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        return this;
    }

    public ItemFactory setLore(String... lore) {
        meta.setLore(Arrays.stream(lore)
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList()));
        return this;
    }

    /**
     * [신규 추가] 기존 로어에 새로운 줄을 추가합니다.
     * (RecipeBookGUI에서 사용)
     * @param lore 추가할 로어
     * @return
     */
    public ItemFactory addLore(String... lore) {
        List<String> currentLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        for (String line : lore) {
            currentLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(currentLore);
        return this;
    }

    public ItemFactory setCustomModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    public ItemFactory addEnchant(Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
        meta.addEnchant(enchantment, level, ignoreLevelRestriction);
        return this;
    }

    public ItemFactory setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemFactory addItemFlags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    /**
     * [추가됨] 레시피 시스템 호환성을 위한 NBT 태그 설정 메서드
     * @param id 커스텀 아이템 ID
     * @return ItemFactory
     */
    public ItemFactory setCustomItemId(String id) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        // ItemManager의 정적(static) 키를 참조
        container.set(ItemManager.CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING, id);
        return this;
    }

    /**
     * 설정된 값들을 바탕으로 최종 ItemStack을 생성합니다.
     * @return 완성된 ItemStack
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 설정된 값들을 바탕으로, 지정된 개수의 ItemStack을 생성합니다.
     * @param amount 아이템 개수
     * @return 완성된 ItemStack
     */
    public ItemStack build(int amount) {
        item.setItemMeta(meta);
        item.setAmount(amount);
        return item;
    }
}