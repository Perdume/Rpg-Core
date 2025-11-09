package Perdume.rpg.core.item.crafting;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.ItemManager;
import Perdume.rpg.core.item.crafting.recipe.*;
// [추가] Bukkit 레시피 등록에 필요한 import
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
// [끝]
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 'recipes.yml' 파일을 로드하고,
 * 커스텀 조합법을 관리하는 매니저 클래스입니다.
 */
public class RecipeManager {

    private final Rpg plugin;
    private final ItemManager itemManager;
    private final Map<String, CustomRecipe> recipes = new HashMap<>();

    public RecipeManager(Rpg plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    /**
     * 'recipes.yml' 파일에서 모든 조합법을 로드하여 'recipes' 맵에 등록합니다.
     * 서버 시작 시 또는 /rpg reload 시 호출됩니다.
     */
    public void loadRecipes() {
        recipes.clear();

        File recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
        if (!recipesFile.exists()) {
            plugin.saveResource("recipes.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(recipesFile);
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null) {
            plugin.getLogger().info("No 'recipes' section found in recipes.yml.");
            return;
        }

        for (String recipeId : recipesSection.getKeys(false)) {
            ConfigurationSection recipeSection = recipesSection.getConfigurationSection(recipeId);
            if (recipeSection == null) continue;

            try {
                // 1. 결과 아이템 파싱
                String resultId = recipeSection.getString("result-id");
                int resultAmount = recipeSection.getInt("result-amount", 1);
                if (resultId == null) {
                    plugin.getLogger().warning("Recipe " + recipeId + " is missing 'result-id'. Skipping.");
                    continue;
                }

                ItemStack resultItem = itemManager.getItem(resultId);
                if (resultItem == null) {
                    plugin.getLogger().warning("Recipe " + recipeId + " has an invalid item-id: " + resultId + ". Skipping.");
                    continue;
                }
                resultItem.setAmount(resultAmount);

                // [추가] Bukkit 레시피북에 등록하기 위한 고유 키 생성
                NamespacedKey recipeKey = new NamespacedKey(plugin, recipeId);

                String type = recipeSection.getString("type", "SHAPED").toUpperCase();

                // 2. 조합법 타입에 따라 파싱
                if (type.equals("SHAPED")) {
                    // --- Shaped Recipe (형태가 있는 조합) ---
                    List<String> shape = recipeSection.getStringList("shape");
                    ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
                    if (shape.isEmpty() || ingredientsSection == null) {
                        plugin.getLogger().warning("Shaped recipe " + recipeId + " is missing 'shape' or 'ingredients'. Skipping.");
                        continue;
                    }

                    // 재료 맵 (A: IngredientBlueprint, B: IngredientBlueprint...)
                    Map<Character, IngredientBlueprint> ingredientMap = new HashMap<>();
                    for (String key : ingredientsSection.getKeys(false)) {
                        char c = key.charAt(0);
                        ConfigurationSection ingSection = ingredientsSection.getConfigurationSection(key);
                        if (ingSection == null) continue;

                        String itemId = ingSection.getString("item-id");
                        int amount = ingSection.getInt("amount", 1);
                        ItemStack ingItem = itemManager.getItem(itemId);

                        if (ingItem == null) {
                            plugin.getLogger().warning("Invalid ingredient item-id: " + itemId + " for key '" + c + "' in recipe " + recipeId);
                            throw new IllegalArgumentException("Invalid ingredient item-id"); // 이 레시피 로드 중단
                        }
                        ingredientMap.put(c, new IngredientBlueprint(ingItem, amount));
                    }

                    // 슬롯 맵 (0: IngredientBlueprint, 1: IngredientBlueprint...)
                    Map<Integer, IngredientBlueprint> slotIngredients = new HashMap<>();
                    int slotIndex = 0; // 0-8 (CraftingInventory 슬롯 인덱스)
                    for (String row : shape) {
                        for (char c : row.toCharArray()) {
                            if (slotIndex >= 9) {
                                break;
                            }
                            if (c != ' ') {
                                IngredientBlueprint blueprint = ingredientMap.get(c);
                                if (blueprint == null) {
                                    plugin.getLogger().warning("Shape key '" + c + "' in recipe " + recipeId + " is not defined in ingredients. Skipping.");
                                    throw new IllegalArgumentException("Shape key not defined.");
                                }
                                slotIngredients.put(slotIndex, blueprint);
                            }
                            // [버그 수정] slotIndex가 ' ' 공백에서도 증가하도록 수정 (이전 커밋에서 수정됨)
                            slotIndex++;
                        }
                        if (slotIndex >= 9) {
                            break;
                        }
                    }

                    while (slotIndex < 9) {
                        slotIndex++;
                    }

                    // --- 1. 우리 커스텀 시스템에 등록 ---
                    ShapedRecipeBlueprint recipe = new ShapedRecipeBlueprint(resultItem, slotIngredients);
                    recipes.put(recipeId, recipe);
                    // [제거] Shaped 레시피 등록 성공 로그

                    // --- 2. [추가] Bukkit 레시피북에 등록 ---
                    // (서버에 이미 등록된 레시피는 덮어쓰지 않도록 확인)
                    if (Bukkit.getRecipe(recipeKey) == null) {
                        ShapedRecipe bukkitRecipe = new ShapedRecipe(recipeKey, resultItem);
                        bukkitRecipe.shape(shape.toArray(new String[0]));

                        for (Map.Entry<Character, IngredientBlueprint> entry : ingredientMap.entrySet()) {
                            ItemStack ingStack = entry.getValue().getItemStack().clone();
                            // RecipeChoice는 재료 1개 기준이어야 하므로 1로 설정 (어차피 우리 리스너가 수량을 다시 검사함)
                            ingStack.setAmount(1);
                            // ExactChoice는 NBT 태그까지 정확하게 일치하는 아이템을 요구
                            bukkitRecipe.setIngredient(entry.getKey(), new RecipeChoice.ExactChoice(ingStack));
                        }
                        Bukkit.addRecipe(bukkitRecipe);
                        // [제거] Bukkit 레시피북 등록 성공 로그
                    }


                } else if (type.equals("SHAPELESS")) {
                    // --- Shapeless Recipe (형태가 없는 조합) ---
                    List<Map<?, ?>> ingredientsList = recipeSection.getMapList("ingredients");
                    if (ingredientsList.isEmpty()) {
                        plugin.getLogger().warning("Shapeless recipe " + recipeId + " is missing 'ingredients' list. Skipping.");
                        continue;
                    }

                    List<IngredientBlueprint> requiredIngredients = new ArrayList<>();
                    for (Map<?, ?> ingredientMap : ingredientsList) {
                        String itemId = (String) ingredientMap.get("item-id");

                        int amount = 1;
                        Object amountObj = ingredientMap.get("amount");
                        if (amountObj instanceof Integer) {
                            amount = (Integer) amountObj;
                        }

                        ItemStack ingItem = itemManager.getItem(itemId);
                        if (ingItem == null) {
                            plugin.getLogger().warning("Invalid ingredient item-id: " + itemId + " in shapeless recipe " + recipeId);
                            throw new IllegalArgumentException("Invalid ingredient item-id"); // 이 레시피 로드 중단
                        }
                        requiredIngredients.add(new IngredientBlueprint(ingItem, amount));
                    }

                    // --- 1. 우리 커스텀 시스템에 등록 ---
                    ShapelessRecipeBlueprint recipe = new ShapelessRecipeBlueprint(resultItem, requiredIngredients);
                    recipes.put(recipeId, recipe);
                    // [제거] Shapeless 레시피 등록 성공 로그

                    // --- 2. [추가] Bukkit 레시피북에 등록 ---
                    if (Bukkit.getRecipe(recipeKey) == null) {
                        ShapelessRecipe bukkitRecipe = new ShapelessRecipe(recipeKey, resultItem);
                        for (IngredientBlueprint blueprint : requiredIngredients) {
                            ItemStack ingStack = blueprint.getItemStack().clone();
                            ingStack.setAmount(1); // 1개로 설정
                            bukkitRecipe.addIngredient(new RecipeChoice.ExactChoice(ingStack));
                        }
                        Bukkit.addRecipe(bukkitRecipe);
                        // [제거] Bukkit 레시피북 등록 성공 로그
                    }
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load recipe: " + recipeId + ". Error: " + e.getMessage());
                e.printStackTrace(); // [유지] 오류 추적을 위해 스택 트레이스 유지
            }
        }

        plugin.getLogger().info("Successfully loaded " + recipes.size() + " custom recipes."); // [유지] 최종 로드 성공 로그
    }

    public Map<String, CustomRecipe> getRecipes() {
        return recipes;
    }
    public ItemManager getItemManager() {
        return this.itemManager;
    }
}