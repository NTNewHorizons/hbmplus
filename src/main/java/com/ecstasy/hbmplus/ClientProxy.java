package com.ecstasy.hbmplus;

import java.util.Iterator;

import com.ecstasy.hbmplus.Machines.MachineShredderLarge.RenderMachineShredder;
import com.ecstasy.hbmplus.Machines.MachineShredderLarge.TileEntityMachineShredderLarge;
import com.hbm.main.ModEventHandlerRenderer;
import com.hbm.main.ServerProxy;
import com.hbm.render.loader.HmfModelLoader;
import com.hbm.render.tileentity.IItemRendererProvider;
import com.hbm.render.util.RenderInfoSystem;
import com.hbm.wiaj.cannery.Jars;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends ServerProxy {

    public RenderInfoSystem theInfoSystem = new RenderInfoSystem();
	
	@Override public void registerRenderInfo() {

//		registerClientEventHandler(new ModEventHandlerClient()); <-- causes double tooltips
		registerClientEventHandler(new ModEventHandlerRenderer());
		registerClientEventHandler(theInfoSystem);

		AdvancedModelLoader.registerModelHandler(new HmfModelLoader());
		registerTileEntitySpecialRenderer();
		registerItemRenderer();
		
		Jars.initJars();
	}

	@Override public void registerItemRenderer() {
		@SuppressWarnings("rawtypes")
		Iterator iterator = TileEntityRendererDispatcher.instance.mapSpecialRenderers.values().iterator();

		while(iterator.hasNext()) {
			Object renderer = iterator.next();
			if(renderer instanceof IItemRendererProvider) {
				IItemRendererProvider prov = (IItemRendererProvider) renderer;
				
				for(Item item : prov.getItemsForRenderer()) {
					MinecraftForgeClient.registerItemRenderer(item, prov.getRenderer());
				}
			}
		}
	}

    private void registerClientEventHandler(Object handler) {
		MinecraftForge.EVENT_BUS.register(handler);
		FMLCommonHandler.instance().bus().register(handler);
	}
    
    @Override public void registerTileEntitySpecialRenderer() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityMachineShredderLarge.class, new RenderMachineShredder());
	}
}
