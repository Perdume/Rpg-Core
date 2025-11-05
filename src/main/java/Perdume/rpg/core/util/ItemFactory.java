package Perdume.rpg.core.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 빌더 패턴을 사용하여 ItemStack을 보다 편리하게 생성하는 유틸리티 클래스입니다.
 */
public class ItemFactory {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemFactory(Material material) {
        this.item = new ItemStack(material);
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