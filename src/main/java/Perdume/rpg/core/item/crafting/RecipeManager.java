package Perdume.rpg.core.item.crafting;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.CustomItemUtil;
import Perdume.rpg.core.item.TwistedCobblestone;
import Perdume.rpg.core.item.crafting.recipe.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [YML 파싱 엔진] recipes 폴더의 YML 파일들을 읽어와 커스텀 조합법을 로드하고 관리합니다.
 */
public class RecipeManager {

    private final Rpg plugin;
    private final List<CustomRecipe> customRecipes = new ArrayList<>();

    public RecipeManager(Rpg plugin) {
        this.plugin = plugin;
        loadRecipes();
    }

    /**
     * 'plugins/Rpg/recipes' 폴더에서 모든 .yml 조합법 파일을 로드합니다.
     */
    public void loadRecipes() {
        customRecipes.clear();
        File recipesFolder = new File(plugin.getDataFolder(), "recipes");
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
            // 예시 YML 파일을 생성해줄 수도 있습니다.
            plugin.saveResource("recipes/example_recipe.yml", false);
        }

        File[] recipeFiles = recipesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (recipeFiles == null) return;

        for (File file : recipeFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            try {
                CustomRecipe recipe = parseRecipe(config);
                if (recipe != null) {
                    customRecipes.add(recipe);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("조합법 파일을 파싱하는 중 오류 발생: " + file.getName());
                e.printStackTrace();
            }
        }
        plugin.getLogger().info(customRecipes.size() + "개의 커스텀 조합법을 로드했습니다.");
    }

    /**
     * YML config로부터 CustomRecipe 객체를 파싱합니다.
     */
    private CustomRecipe parseRecipe(FileConfiguration config) {
        ItemStack result = parseResult(config.getConfigurationSection("result"));
        if (result == null) throw new IllegalArgumentException("결과물(result)이 잘못되었습니다.");

        String type = config.getString("type", "SHAPED").toUpperCase();

        if (type.equals("SHAPED")) {
            return parseShapedRecipe(result, config.getConfigurationSection("slots"));
        } else if (type.equals("SHAPELESS")) {
            return parseShapelessRecipe(result, config.getList("ingredients"));
        }
        return null;
    }

    private ShapedRecipeBlueprint parseShapedRecipe(ItemStack result, ConfigurationSection slotsSection) {
        if (slotsSection == null) throw new IllegalArgumentException("Shaped 레시피에 'slots' 섹션이 없습니다.");
        Map<Integer, IngredientBlueprint> slotIngredients = new HashMap<>();
        for (String key : slotsSection.getKeys(false)) {
            int slotNumber = Integer.parseInt(key);
            if (slotNumber < 1 || slotNumber > 9) continue;

            ConfigurationSection ingredientSection = slotsSection.getConfigurationSection(key);
            slotIngredients.put(slotNumber, parseIngredient(ingredientSection));
        }
        return new ShapedRecipeBlueprint(result, slotIngredients);
    }

    private ShapelessRecipeBlueprint parseShapelessRecipe(ItemStack result, List<?> ingredientsList) {
        if (ingredientsList == null) throw new IllegalArgumentException("Shapeless 레시피에 'ingredients' 목록이 없습니다.");
        List<IngredientBlueprint> ingredients = new ArrayList<>();
        for (Object obj : ingredientsList) {
            if (obj instanceof Map) {
                // Bukkit이 YML을 읽으면 Map이 됩니다. 이를 ConfigurationSection처럼 다루기 위해 변환합니다.
                ConfigurationSection ingredientSection = YamlConfiguration.loadConfiguration(new File("dummy"))
                        .createSection("dummy", (Map<?, ?>) obj);
                ingredients.add(parseIngredient(ingredientSection));
            }
        }
        return new ShapelessRecipeBlueprint(result, ingredients);
    }

    /**
     * YML의 재료 섹션(Map)을 IngredientBlueprint 객체로 파싱합니다.
     */
    private IngredientBlueprint parseIngredient(ConfigurationSection section) {
        String type = section.getString("type", "VANILLA").toUpperCase();
        int amount = section.getInt("amount", 1);

        if (type.equals("CUSTOM")) {
            String id = section.getString("id");
            if (id == null) throw new IllegalArgumentException("CUSTOM 재료에 'id'가 없습니다.");
            return new IngredientBlueprint(id, amount);
        } else { // VANILLA
            String materialName = section.getString("material");
            if (materialName == null) throw new IllegalArgumentException("VANILLA 재료에 'material'이 없습니다.");
            Material material = Material.matchMaterial(materialName.toUpperCase());
            if (material == null) throw new IllegalArgumentException("알 수 없는 Material: " + materialName);
            return new IngredientBlueprint(material, amount);
        }
    }

    /**
     * YML의 결과물 섹션(Map)을 ItemStack 객체로 파싱합니다.
     */
    private ItemStack parseResult(ConfigurationSection section) {
        if (section == null) return null;
        String type = section.getString("type", "VANILLA").toUpperCase();
        int amount = section.getInt("amount", 1);

        if (type.equals("CUSTOM")) {
            String id = section.getString("id");
            if (id == null) throw new IllegalArgumentException("CUSTOM 결과물에 'id'가 없습니다.");
            
            // ItemManager에서 ID로 아이템을 가져와야 함. (지금은 TwistedCobblestone만 임시로 체크)
            // TODO: ItemManager와 연동 필요
            for(TwistedCobblestone stone : TwistedCobblestone.values()) {
                if (stone.name().toLowerCase().equals(id.replace("twisted_cobblestone_t", "tier_"))) {
                    return stone.getItemStack(amount);
                }
            }
            // 임시: 찾지 못하면 돌 반환
            ItemStack item = new ItemStack(Material.STONE, amount);
            CustomItemUtil.setCustomId(item, id);
            return item;

        } else { // VANILLA
            String materialName = section.getString("material");
            if (materialName == null) throw new IllegalArgumentException("VANILLA 결과물에 'material'이 없습니다.");
            Material material = Material.matchMaterial(materialName.toUpperCase());
            if (material == null) throw new IllegalArgumentException("알 수 없는 Material: " + materialName);
            return new ItemStack(material, amount);
        }
    }

    /**
     * 현재 조합창과 일치하는 커스텀 레시피를 찾습니다.
     * @param inventory 조합창
     * @return 일치하는 CustomRecipe, 없으면 null
     */
    public CustomRecipe getMatchingRecipe(CraftingInventory inventory) {
        for (CustomRecipe recipe : customRecipes) {
            if (recipe.matches(inventory)) {
                return recipe;
            }
        }
        return null;
    }
}

