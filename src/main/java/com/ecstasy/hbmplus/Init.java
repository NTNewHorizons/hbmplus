package com.ecstasy.hbmplus;

import com.ecstasy.hbmplus.Recipes.ICFLaserMulti;
import com.ecstasy.hbmplus.Recipes.InitRecipes;
import com.hbm.main.ServerProxy;
import com.ecstasy.hbmplus.AE2.InitAE2;
import com.ecstasy.hbmplus.Blocks.InitBlocks;
import com.ecstasy.hbmplus.Fluids.CustomFluids;
import com.ecstasy.hbmplus.Items.InitItems;
import com.ecstasy.hbmplus.Machines.MachineShredderLarge.TileEntityMachineShredderLarge;
import com.ecstasy.hbmplus.Machines.MegaCooler.TileEntityMegaCooler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = Init.MODID, version = Init.VERSION, name = Init.NAME)

public class Init {
    public static final String MODID = "hmbplus";//bro fml i did a typo im not reverting it because then everything breaks
    public static final String NAME = "HBM Plus";
    public static final String VERSION = "1.1";

    @Instance(MODID)
    public static Init instance;

    @SidedProxy(clientSide = "com.ecstasy.hbmplus.ClientProxy", serverSide = "com.ecstasy.hbmplus.CommonProxy")
	public static ServerProxy proxy;

    @EventHandler public void preInit(FMLPreInitializationEvent event) {
        InitAE2.Init();
        InitItems.Main();
        InitBlocks.Init();

        proxy.registerRenderInfo();
    }

    @EventHandler public void init(FMLInitializationEvent event) {
        CustomFluids.init();

        GameRegistry.registerTileEntity(TileEntityMegaCooler.class, MODID + "tileentity_mega_cooler");
        GameRegistry.registerTileEntity(TileEntityMachineShredderLarge.class, MODID + "tileentity_machine_big_shredder");
    }

    @EventHandler public void postInit(FMLPostInitializationEvent event) throws IllegalArgumentException, IllegalAccessException {
        InitRecipes.Main();
        ICFLaserMulti.patch();
    }
}
