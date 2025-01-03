package com.ecstasy.hbmplus.Items;

import com.ecstasy.hbmplus.Shared.ModLogger;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.ModItems;
import com.hbm.main.CraftingManager;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.ecstasy.hbmplus.Util.CustomHazard;

public class ItemCustomBreedingRod extends Item {

    public Item quadRod;
    public Item dualRod;
    public Item rod;

    public ItemCustomBreedingRod(String name, Item billet, float rad, int id) {
        ModLogger.logger.info("Lmao");
        
        this.rod = new CustomItem("rod_" + name).setContainerItem(ModItems.rod_empty);
        this.dualRod = new CustomItem("rod_dual_" + name).setContainerItem(ModItems.rod_dual_empty);
        this.quadRod = new CustomItem("rod_quad_" + name).setContainerItem(ModItems.rod_quad_empty);

        CraftingManager.addShapelessAuto(new ItemStack(rod, 1, 0), new Object[] { ModItems.rod_empty, billet });
        CraftingManager.addShapelessAuto(new ItemStack(billet, 1, 0), new Object[] { rod });

        CraftingManager.addShapelessAuto(new ItemStack(dualRod, 1, 0), new Object[] { ModItems.rod_dual_empty, billet, billet });
        CraftingManager.addShapelessAuto(new ItemStack(billet, 2, 0), new Object[] { dualRod });

        CraftingManager.addShapelessAuto(new ItemStack(quadRod, 1, 0), new Object[] { ModItems.rod_quad_empty, billet, billet, billet, billet });
        CraftingManager.addShapelessAuto(new ItemStack(billet, 4, 0), new Object[] { quadRod });

        GameRegistry.registerItem(rod, rod.getUnlocalizedName());
        GameRegistry.registerItem(dualRod, dualRod.getUnlocalizedName());
        GameRegistry.registerItem(quadRod, quadRod.getUnlocalizedName());

        HazardSystem.register(rod, CustomHazard.makeData(ItemsMod.RADIATION, rad * ItemsMod.billet) );
        HazardSystem.register(dualRod, CustomHazard.makeData(ItemsMod.RADIATION, rad * ItemsMod.billet * 2) );
        HazardSystem.register(quadRod, CustomHazard.makeData(ItemsMod.RADIATION, rad * ItemsMod.billet * 4) );
    }

}
