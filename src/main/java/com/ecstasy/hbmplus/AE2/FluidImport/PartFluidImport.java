package com.ecstasy.hbmplus.AE2.FluidImport;

import java.util.HashSet;
import java.util.Set;

import com.ecstasy.hbmplus.Shared.ModLogger;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIdentifier;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.fluid.IFluidConductor;
import api.hbm.fluid.IFluidConnector;
import api.hbm.fluid.IFluidStandardTransceiver;
import api.hbm.fluid.IPipeNet;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartRenderHelper;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.texture.CableBusTextures;
import appeng.core.settings.TickRates;
import appeng.core.sync.GuiBridge;
import appeng.helpers.Reflected;
import appeng.parts.automation.PartSharedItemBus;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class PartFluidImport extends PartSharedItemBus implements IFluidStandardTransceiver {
    private final BaseActionSource mySrc;
    private boolean didSomething = false;
    private int nextSlot = 0;

    private FluidTank tank;
    public boolean isLoaded = true;
    private static final int FLUID_FILTER_SLOT = 0;

	@Override
	public boolean isLoaded() {
		return isLoaded;
	}

    @Override public FluidTank[] getSendingTanks() {
        return new FluidTank[0];
    }

    @Override public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank };
    }

    @Override public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tank };
    }

    @Override
	public long transferFluid(FluidType type, int pressure, long fluid) {
		long toTransfer = Math.min(getDemand(type, pressure), fluid);
		tank.setFill(tank.getFill() + (int) toTransfer);
//		this.markChanged();
		return fluid - toTransfer;
	}



    @Reflected
    public PartFluidImport(final ItemStack is) {
        super(is);

        this.getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.getConfigManager().registerSetting(Settings.CRAFT_ONLY, YesNo.NO);
        this.getConfigManager().registerSetting(Settings.SCHEDULING_MODE, SchedulingMode.DEFAULT);
        this.mySrc = new MachineSource(this);
        this.tank = new FluidTank(Fluids.NONE, 24000);
    }

     @Override
    public void readFromNBT(final NBTTagCompound extra) {
        super.readFromNBT(extra);
        this.nextSlot = extra.getInteger("nextSlot");
    }

    @Override
    public void writeToNBT(final NBTTagCompound extra) {
        super.writeToNBT(extra);
        extra.setInteger("nextSlot", this.nextSlot);
    }

    private FluidType getFluidTypeFromFilterSlot() {
        ItemStack filterStack = this.getInventoryByName("config").getStackInSlot(FLUID_FILTER_SLOT);

        if (filterStack != null && filterStack.getItem() instanceof ItemFluidIdentifier) {
            ItemFluidIdentifier id = (ItemFluidIdentifier) filterStack.getItem();
            return id.getType(null, 0, 0, 0, filterStack);
        }
        return Fluids.NONE;
    }

    private DirPos[] getAdjacentPositions() {
        // Get the position in front of the export bus
        int x = this.getHost().getTile().xCoord + this.getSide().offsetX;
        int y = this.getHost().getTile().yCoord + this.getSide().offsetY;
        int z = this.getHost().getTile().zCoord + this.getSide().offsetZ;

        // Create a DirPos array with the adjacent position
        return new DirPos[] { new DirPos(x, y, z, this.getSide()) };
    }

    @SuppressWarnings("unused")
    private int transmitFluid() {
        // Prepare parameters
        World world = this.getHost().getTile().getWorldObj();
        DirPos[] positions = getAdjacentPositions();

        // Transfer fluids
        int fill = tank.getFill();
        fill = transmitFluidFairly(
            world, tank, this, fill, true, false, positions
        );

        return fill;
    }


    private static int transmitFluidFairly(World world, FluidTank tank, IFluidConnector that, int fill, boolean connect, boolean send, DirPos[] connections) {

        Set<IPipeNet> nets = new HashSet<>();
        Set<IFluidConnector> consumers = new HashSet<>();
        FluidType type = tank.getTankType();
        int pressure = tank.getPressure();

        for (DirPos pos : connections) {

            TileEntity te = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());

            if (te instanceof IFluidConductor) {
                IFluidConductor con = (IFluidConductor) te;
                if (con.getPipeNet(type) != null) {
                    nets.add(con.getPipeNet(type));
                    con.getPipeNet(type).unsubscribe(that);
                    consumers.addAll(con.getPipeNet(type).getSubscribers());
                }

            } else if (te instanceof IFluidConnector) {
                consumers.add((IFluidConnector) te);
            }
        }

        consumers.remove(that);

        if (fill > 0 && send) {
            // Send fluids to consumers
            for (IFluidConnector consumer : consumers) {
                if (consumer != null && consumer != that) {
                    long demand = consumer.getDemand(type, pressure);
                    if (demand > 0) {
                        long toSend = Math.min(fill, demand);
                        long remaining = consumer.transferFluid(type, pressure, toSend);
                        long sent = toSend - remaining;
                        fill -= sent;
                        if (fill <= 0) {
                            break;
                        }
                    }
                }
            }
        }

        // Resubscribe to buffered nets, if necessary
        if (connect) {
            for (IPipeNet net : nets) {
                net.subscribe(that);
            }
        }

        return fill;
    }


    @Override
protected TickRateModulation doBusWork() {
    if (!this.getProxy().isActive() || !this.canDoBusWork()) {
        return TickRateModulation.IDLE;
    }

    this.didSomething = false;
    try {
        final InventoryAdaptor destination = this.getHandler();
        final IMEMonitor<IAEItemStack> itemInventory = this.getProxy().getStorage().getItemInventory();

        if (destination != null) {
        
            FluidType fluidTypeToExport = getFluidTypeFromFilterSlot();

            tank.setTankType(fluidTypeToExport);

            ModLogger.logger.info(tank.getFill()+"fill");
            ModLogger.logger.info(fluidTypeToExport.getName());

            if (fluidTypeToExport != Fluids.NONE) {

                DirPos[] positions = getAdjacentPositions();


                for (DirPos pos : positions) {
                    this.trySubscribe(fluidTypeToExport, this.getTile().getWorldObj(), pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
                }

                this.subscribeToAllAround(fluidTypeToExport, this.getHost().getTile());

                long fluidAmountToExport = getDemand(fluidTypeToExport, tank.getPressure());
                ModLogger.logger.info(fluidAmountToExport+"ending my shoit");
                ModLogger.logger.info(tank.getFill()+"hiiiii");

                if (fluidAmountToExport > 0) {
                    int remainingFluid = transmitFluidFairly(this.getHost().getTile().getWorldObj(), tank, this, tank.getFill(), true, true, positions);
                    tank.setFill(remainingFluid);
                    ModLogger.logger.info(remainingFluid+"Ok.");
                    ModLogger.logger.info(tank.getFill()+"FUCK");
//                if (remainingFluid < fluidAmountToExport) {
                    this.didSomething = true;
                    int fluidAmount = tank.getFill();
                    int itemsToCreate = fluidAmount / 1000; // temporary
                    int remainingFluidd = fluidAmount % 1000;

                    if (itemsToCreate > 0) {
                        ItemStack fluidItemStack = new ItemStack(ModItems.fluid_icon, itemsToCreate, fluidTypeToExport.getID());
                        ModLogger.logger.info("Creating " + itemsToCreate + " fluid items");
                        IAEItemStack itemStackToInsert = AEItemStack.create(fluidItemStack);
                        IAEItemStack notInserted = itemInventory.injectItems(itemStackToInsert, Actionable.SIMULATE, mySrc);

                        if (notInserted == null || notInserted.getStackSize() == 0) {
                            // all items can be inserted
                            itemInventory.injectItems(itemStackToInsert, Actionable.MODULATE, mySrc);
                            // Remove the fluid from the tank
                            ModLogger.logger.info("All items inserted successfully");
                            tank.setFill(remainingFluidd);
                            this.didSomething = true;
                        } else {
                            // mot all items could be inserted, adjust accordingly
                            int itemsInserted = (int) (itemsToCreate - notInserted.getStackSize());
                            ModLogger.logger.info("Only " + itemsInserted + " items could be inserted");

                            if (itemsInserted > 0) {
                                ModLogger.logger.info("Creating partial fluid items: " + itemsInserted);
                                ItemStack insertedStack = new ItemStack(ModItems.fluid_icon, itemsInserted, fluidTypeToExport.getID());
                                IAEItemStack insertedAEStack = AEItemStack.create(insertedStack);
                                itemInventory.injectItems(insertedAEStack, Actionable.MODULATE, mySrc);

                                // remove the corresponding fluid from the tank
                                tank.setFill(remainingFluidd);
                                this.didSomething = true;
                            }
                        }
 //              } else {
 //                   ModLogger.logger.info("No fluid received this tick.");
 //                   return TickRateModulation.SLOWER; // No fluid received
//                }

                }
            }

    
            
        }


    } else {
        return TickRateModulation.SLEEP;
    }
    } catch (Exception e) {
            e.printStackTrace();
    }

    return this.didSomething ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
}

    @Override
    public void getBoxes(final IPartCollisionHelper bch) {
        bch.addBox(4, 4, 12, 12, 12, 14);
        bch.addBox(5, 5, 14, 11, 11, 15);
        bch.addBox(6, 6, 15, 10, 10, 16);
        bch.addBox(6, 6, 11, 10, 10, 12);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventory(final IPartRenderHelper rh, final RenderBlocks renderer) {
        rh.setTexture(
                CableBusTextures.PartExportSides.getIcon(),
                CableBusTextures.PartExportSides.getIcon(),
                CableBusTextures.PartMonitorBack.getIcon(),
                this.getItemStack().getIconIndex(),
                CableBusTextures.PartExportSides.getIcon(),
                CableBusTextures.PartExportSides.getIcon());

        rh.setBounds(4, 4, 12, 12, 12, 14);
        rh.renderInventoryBox(renderer);

        rh.setBounds(5, 5, 14, 11, 11, 15);
        rh.renderInventoryBox(renderer);

        rh.setBounds(6, 6, 15, 10, 10, 16);
        rh.renderInventoryBox(renderer);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(final int x, final int y, final int z, final IPartRenderHelper rh,
            final RenderBlocks renderer) {
        this.setRenderCache(rh.useSimplifiedRendering(x, y, z, this, this.getRenderCache()));
        rh.setTexture(
                CableBusTextures.PartExportSides.getIcon(),
                CableBusTextures.PartExportSides.getIcon(),
                CableBusTextures.PartMonitorBack.getIcon(),
                this.getItemStack().getIconIndex(),
                CableBusTextures.PartExportSides.getIcon(),
                CableBusTextures.PartExportSides.getIcon());

        rh.setBounds(4, 4, 12, 12, 12, 14);
        rh.renderBlock(x, y, z, renderer);

        rh.setBounds(5, 5, 14, 11, 11, 15);
        rh.renderBlock(x, y, z, renderer);

        rh.setBounds(6, 6, 15, 10, 10, 16);
        rh.renderBlock(x, y, z, renderer);

        rh.setTexture(
                CableBusTextures.PartMonitorSidesStatus.getIcon(),
                CableBusTextures.PartMonitorSidesStatus.getIcon(),
                CableBusTextures.PartMonitorBack.getIcon(),
                this.getItemStack().getIconIndex(),
                CableBusTextures.PartMonitorSidesStatus.getIcon(),
                CableBusTextures.PartMonitorSidesStatus.getIcon());

        rh.setBounds(6, 6, 11, 10, 10, 12);
        rh.renderBlock(x, y, z, renderer);

        this.renderLights(x, y, z, rh, renderer);
    }

    @Override
    public int cableConnectionRenderTo() {
        return 5;
    }

    @Override
    public boolean onPartActivate(final EntityPlayer player, final Vec3 pos) {
        if (!player.isSneaking()) {
            if (Platform.isClient()) {
                return true;
            }

            Platform.openGUI(player, this.getHost().getTile(), this.getSide(), GuiBridge.GUI_BUS);
            return true;
        }

        return false;
    }

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        return new TickingRequest(TickRates.ExportBus.getMin(), TickRates.ExportBus.getMax(), this.isSleeping(), false);
    }

    @Override
    public RedstoneMode getRSMode() {
        return (RedstoneMode) this.getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED);
    }

    @Override
    public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
        return this.doBusWork();
    }

    @Override
    protected boolean isSleeping() {
        return this.getHandler() == null || super.isSleeping();
    }


}
