package com.ecstasy.hbmplus.Recipes;

import com.hbm.inventory.recipes.ChemicalPlantRecipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;

import net.minecraft.item.ItemStack;

import com.hbm.inventory.FluidStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.Fluids;

public class ChemPlant {

    public static void Init () {
        final int powerConsumption = 1_000;
        final GenericRecipe ENCHANTMENT = new GenericRecipe("chem.enchantment").setup(5 * 20, powerConsumption);
        final GenericRecipe EXPERIENCE = new GenericRecipe("chem.experience").setup(3 * 20, powerConsumption);

        ENCHANTMENT.inputItems(new ComparableStack(ModItems.powder_quartz, 4));
        ENCHANTMENT.inputFluids(new FluidStack(Fluids.XENON, 1000));
        ENCHANTMENT.outputItems(new ItemStack(ModItems.powder_magic, 1));

        EXPERIENCE.inputItems(new ComparableStack(ModItems.powder_magic), new ComparableStack(ModItems.powder_lapis, 4));
        EXPERIENCE.outputFluids(new FluidStack(Fluids.XPJUICE, 1000));

        ChemicalPlantRecipes.INSTANCE.register(ENCHANTMENT);
        ChemicalPlantRecipes.INSTANCE.register(EXPERIENCE);
    }
}
