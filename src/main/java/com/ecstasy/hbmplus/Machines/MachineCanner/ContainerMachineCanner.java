package com.ecstasy.hbmplus.Machines.MachineCanner;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

public class ContainerMachineCanner extends Container {

    private TileEntityMachineCanner machine;
    public ContainerMachineCanner(InventoryPlayer invPlayer, TileEntityMachineCanner tileEntityMachineShredderLarge) {

    }

    @Override public boolean canInteractWith(EntityPlayer player) {
        return machine.isUseableByPlayer(player);
    }
}