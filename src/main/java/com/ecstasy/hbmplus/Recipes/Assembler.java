package com.ecstasy.hbmplus.Recipes;

import com.ecstasy.hbmplus.Shared.Shared;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.ecstasy.hbmplus.Blocks.InitBlocks;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemCircuit.EnumCircuitType;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Assembler {

    static class NoAnvilRecipeException extends RuntimeException {
        private static final String msg = "No anvil recipe found for output: ";
        public NoAnvilRecipeException(Item anvilOutput) { super(msg + anvilOutput.getUnlocalizedName()); }
    }

    static AnvilConstructionRecipe findAnvilRecipeFromOutput(Item anvilOutput) {

        for (AnvilConstructionRecipe recipe : AnvilRecipes.constructionRecipes) {

            Item out = recipe.output.get(0).stack.getItem();

            if (out == anvilOutput) {
                return recipe;   // ← FOUND IT
            }
        }

        throw new NoAnvilRecipeException(anvilOutput);
    }

    //the game errors for anvil recipes requiring >64 of an ingredient
    static AStack[] splitAStacks(AStack[] input) {
        List<AStack> out = new ArrayList<>();

        for (AStack stack : input) {
            int count = stack.stacksize;
            int max = 64;

            // full stacks
            while (count > max) {
                out.add(stack.copy(max));
                count -= max;
            }

            // remainder
            if (count > 0) {
                out.add(stack.copy(count));
            }
        }

        return out.toArray(new AStack[0]);
    }


    static GenericRecipe fromAnvilRecipe(String id, Item anvilOutput, int durationTicks) {

        AnvilConstructionRecipe anvil = findAnvilRecipeFromOutput(anvilOutput);

        //haha ass
        GenericRecipe recipe = new GenericRecipe("ass." + id).setDuration(durationTicks);

        AStack[] anvilInputs = anvil.input.toArray(new AStack[0]);
        AStack[] assInputs = splitAStacks(anvilInputs);

        recipe.inputItems(assInputs);
        recipe.outputItems(anvil.output.get(0).stack);

        //register
        AssemblyMachineRecipes.INSTANCE.register(recipe);

        return recipe;
    }

    static GenericRecipe fromAnvilRecipe(String id, Block anvilOutput, int durationTicks) {
        return fromAnvilRecipe(id, new ItemStack(anvilOutput).getItem(), durationTicks);
    }
    static GenericRecipe fromAnvilRecipe(String id, Block anvilOutput) {
        return fromAnvilRecipe(id, anvilOutput, 10 * 20);
    }

    static Block getMaybeBlock(String name) {
        try {
            return (Block) ModBlocks.class.getField(name).get(null);
        } catch (Exception e) {
            return null; // field does not exist or not accessible
        }
    }


    public static void Init() {

        final GenericRecipe TSAR_CORE = new GenericRecipe("ass.tsar_core").setDuration(30 * 20);
        TSAR_CORE.outputItems(new ItemStack(ModItems.tsar_core, 1));
        TSAR_CORE.inputItems(
                new OreDictStack(OreDictManager.U238.nugget(), 48),
                new OreDictStack(OreDictManager.IRON.plate(), 24),
                new OreDictStack(OreDictManager.STEEL.plate(), 32),
                new ComparableStack(ModItems.cell_deuterium, 15),
                new ComparableStack(ModItems.powder_lithium, 15)
        );

        final GenericRecipe SHREDDER_LARGE = new GenericRecipe("ass.shredder_large").setDuration(30 * 20);
        SHREDDER_LARGE.outputItems(new ItemStack(Item.getItemFromBlock(InitBlocks.machine_shredder_large), 1));
        SHREDDER_LARGE.inputItems(
                new OreDictStack(OreDictManager.STEEL.plate(), 32),
                new OreDictStack(OreDictManager.ANY_RESISTANTALLOY.ingot(), 24),
                new ComparableStack(ModItems.motor_desh, 12),
                new ComparableStack(ModItems.circuit, 6, EnumCircuitType.ADVANCED.ordinal()),
        );


        AssemblyMachineRecipes.INSTANCE.register(TSAR_CORE);
        AssemblyMachineRecipes.INSTANCE.register(SHREDDER_LARGE);

        //anvil stuff
        fromAnvilRecipe("furnace_combination", ModBlocks.furnace_combination);
        fromAnvilRecipe("furnace_rotary", ModBlocks.machine_rotary_furnace);

        fromAnvilRecipe("machine_industrial_boiler", ModBlocks.machine_industrial_boiler);
        fromAnvilRecipe("machine_deuterium_tower", ModBlocks.machine_deuterium_tower);
        fromAnvilRecipe("machine_tower_large", ModBlocks.machine_tower_large);//cooling tower
        fromAnvilRecipe("machine_tower_small", ModBlocks.machine_tower_small);//auxiliary cooling tower
        fromAnvilRecipe("machine_arc_welder", ModBlocks.machine_arc_welder);
        fromAnvilRecipe("machine_crucible", ModBlocks.machine_crucible);
        fromAnvilRecipe("machine_boiler", ModBlocks.machine_boiler);
        fromAnvilRecipe("machine_ashpit", ModBlocks.machine_ashpit);

        fromAnvilRecipe("heater_oilburner", ModBlocks.heater_oilburner);//fluid burner
        fromAnvilRecipe("heater_electric", ModBlocks.heater_electric);
        fromAnvilRecipe("heater.firebox", ModBlocks.heater_firebox);
        fromAnvilRecipe("heater.heatex", ModBlocks.heater_heatex);
        fromAnvilRecipe("heater.oven", ModBlocks.heater_oven);

        fromAnvilRecipe("pump_electric", ModBlocks.pump_electric);
        fromAnvilRecipe("pump_steam", ModBlocks.pump_steam);

        if (Shared.IS_SPACEFORK) {
            fromAnvilRecipe("machine_atmo_tower", getMaybeBlock("machine_atmo_tower"));//atmo liquefaction machine
            fromAnvilRecipe("machine_atmo_vent", getMaybeBlock("machine_atmo_vent"));//atmo compressor


        }

    }
}
