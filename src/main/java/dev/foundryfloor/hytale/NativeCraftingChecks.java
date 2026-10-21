package dev.foundryfloor.hytale;

import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.bench.CraftingBench;
import java.util.*;

/** Fail startup if the pinned native registry cannot actually offer the intended bench recipes. */
final class NativeCraftingChecks {
    private NativeCraftingChecks() {}
    static void verify() {
        var type=BlockType.getAssetMap().getAsset("FF_FoundryBench");
        if(type==null||!(type.getBench() instanceof CraftingBench bench)||!bench.getId().equals("FoundryFloor")||bench.getCategories().length!=5)throw new IllegalStateException("Foundry Workbench requires its native five-category crafting configuration");
        Set<String> expected=new HashSet<>(FoundryPlugin.ITEMS);expected.remove("FF_FoundryBench");Set<String> actual=new HashSet<>();
        for(var recipe:CraftingPlugin.getBenchRecipes(BenchType.Crafting,"FoundryFloor")) {
            var output=recipe.getPrimaryOutput();if(output==null||output.getItemId()==null||!expected.contains(output.getItemId()))throw new IllegalStateException("Unexpected Foundry bench output");
            if(!actual.add(output.getItemId())||recipe.getInput().length!=1||!"Ingredient_Bar_Iron".equals(recipe.getInput()[0].getItemId())||recipe.getInput()[0].getQuantity()<1||recipe.isKnowledgeRequired())throw new IllegalStateException("Foundry block recipe has invalid acquisition requirements: "+output.getItemId());
        }
        if(!actual.equals(expected))throw new IllegalStateException("Native Foundry Workbench registry does not offer all25 factory block recipes");
        boolean benchRecipe=false;
        for(var recipe:CraftingPlugin.getBenchRecipes(BenchType.Crafting,"Workbench")) {
            var output=recipe.getPrimaryOutput();if(output==null||!"FF_FoundryBench".equals(output.getItemId()))continue;
            Map<String,Integer> inputs=new HashMap<>();for(var input:recipe.getInput())inputs.merge(input.getItemId(),input.getQuantity(),Integer::sum);
            if(!inputs.equals(Map.of("Ingredient_Bar_Iron",8,"Ingredient_Bar_Copper",4))||recipe.isKnowledgeRequired())throw new IllegalStateException("Foundry Workbench must cost8 iron and4 copper at a stock Workbench");benchRecipe=true;
        }
        if(!benchRecipe)throw new IllegalStateException("The Foundry Workbench is not obtainable at the stock Workbench");
    }
}
