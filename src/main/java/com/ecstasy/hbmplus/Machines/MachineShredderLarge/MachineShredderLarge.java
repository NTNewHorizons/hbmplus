package com.ecstasy.hbmplus.Machines.MachineShredderLarge;

import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;

import com.ecstasy.hbmplus.Machines.MachineShredderLarge.TileEntityMachineShredderLarge;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineShredderLarge extends BlockDummyable {

    public MachineShredderLarge() {
        super(Material.iron);
    }

    @Override public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) {
			return true;
		} else if(!player.isSneaking()) {
			int[] pos = this.findCore(world, x, y, z);

			if(pos == null)
				return false;

			FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, pos[0], pos[1], pos[2]);
			return true;
		} else {
			return false;
		}
	}

	// fuck this i just want local positions.. just assume its north or something idk man this sucks
	// UP DOWN FORWARD BACKWARDS LEFT RIGHT
    protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);

		// so i have to do like left = 3 right = -2 to effectively move it while still making it 1 wide.. nice
		MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {3, -1, 1, 1, 3, -2}, this, dir);
		MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {3, -1, 1, 1, -2, 3}, this, dir);
		
		//i need to register ports twice?? i thought the tilentity class decides it? but no both of these classes have to do it
		this.makeExtra(world, x - dir.offsetX + 1, y, z - dir.offsetZ + 1);
		this.makeExtra(world, x - dir.offsetX + 1, y, z - dir.offsetZ - 1);
		this.makeExtra(world, x - dir.offsetX - 1, y, z - dir.offsetZ + 1);
		this.makeExtra(world, x - dir.offsetX - 1, y, z - dir.offsetZ - 1);
	}

	/*
	 X = core
	 P = power input/output
	 F = fluid input/output
	 A = input/output port for anything (item, fluids, power, etc)
	 I = item input
	 O = item output
	 x = structure
	 (number) = direction length for bounding box

	 0, 0, 0 = core position

	 since these are 2d in text, i will say what 2 axis you are looking at, and which one is missing due to it being 2d
	*/

	/* birds eye view (x, z) (y=right above the entire hitbox)

	 	 forwards
			↑ 
			• → right

		3 A O A 3
		3 1 X 1 3
		3 A 1 A 3
				  
	*/

	/* front view (x, y) (z=0)

	   	    up
			↑ 
			• → right

		3		3
		x x 2 x x
		x x 1 x x 
		2 1 C 1 2 
	*/

	// UP DOWN FORWARD BACKWARDS LEFT RIGHT
		
    @Override public int[] getDimensions() {
		return new int[] { 2, 0, 1, 1, 2, 2 };
	}

    @Override public TileEntity createNewTileEntity(World world, int meta) {

		if(meta >= 12)
			return new TileEntityMachineShredderLarge();
		if(meta >= 6)
			return new TileEntityProxyCombo(false, true, true);

		return null;
	}

    @Override public int getOffset() {
		return 1;
	}
}
