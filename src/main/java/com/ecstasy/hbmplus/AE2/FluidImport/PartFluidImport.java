package com.ecstasy.hbmplus.AE2.FluidImport;

import api.hbm.fluidmk2.IFluidReceiverMK2;
import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartRenderHelper;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.texture.CableBusTextures;
import appeng.core.settings.TickRates;
import appeng.helpers.Reflected;
import appeng.parts.automation.PartSharedItemBus;

import com.ecstasy.hbmplus.Integration.HBMFluidBridge;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.items.machine.IItemFluidIdentifier;

import api.hbm.fluidmk2.IFluidUserMK2;
import api.hbm.fluidmk2.IFluidProviderMK2;

import com.hbm.lib.Library;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import appeng.util.Platform;
import appeng.core.sync.GuiBridge;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.entity.player.EntityPlayer;
import appeng.api.config.Actionable;
import appeng.util.item.AEItemStack;
import com.hbm.items.ModItems;
import api.hbm.fluidmk2.FluidNode;
import com.hbm.uninos.UniNodespace;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashSet;
//IFluidUserMK2, IFluidProviderMK2 export
//IFluidUserMK2, IFluidReceiverMK2 import
public class PartFluidImport extends PartSharedItemBus implements IFluidUserMK2, IFluidReceiverMK2 {

    private final MachineSource mySrc;
    private final FluidTank tank;
    private static final int FLUID_FILTER_SLOT = 0;
    private boolean didSomething = false;
    protected FluidNode node;
    protected FluidType lastType;

    private static final int fluidRate = 1;

    @Reflected
    public PartFluidImport(ItemStack is) {
        super(is);
        getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        getConfigManager().registerSetting(Settings.CRAFT_ONLY, YesNo.NO);
        mySrc = new MachineSource(this);
        tank = new FluidTank(Fluids.NONE, 128000);
    }


    @Override
    public boolean isLoaded() { return getHost() != null && getHost().getTile() != null; }

    @Override
    public void readFromNBT(NBTTagCompound extra) {
        super.readFromNBT(extra);
        tank.readFromNBT(extra, "tank");
    }

    @Override
    public void writeToNBT(NBTTagCompound extra) {
        super.writeToNBT(extra);
        tank.writeToNBT(extra, "tank");
    }

    private FluidType getFluidTypeFromFilterSlot() {
        ItemStack filterStack = this.getInventoryByName("config").getStackInSlot(FLUID_FILTER_SLOT);
        if (filterStack != null && filterStack.getItem() instanceof IItemFluidIdentifier) {
            IItemFluidIdentifier id = (IItemFluidIdentifier) filterStack.getItem();
            return id.getType(null, 0, 0, 0, filterStack);
        }
        return Fluids.NONE;
    }

    protected DirPos[] getConPos() {
        ForgeDirection side = this.getSide();
        int nx = this.getHost().getTile().xCoord + side.offsetX;
        int ny = this.getHost().getTile().yCoord + side.offsetY;
        int nz = this.getHost().getTile().zCoord + side.offsetZ;
        return new DirPos[] {
                new DirPos(nx, ny, nz, side)
        };
    }

    protected FluidNode createNode(FluidType type) {
        DirPos[] conPos = getConPos();

        HashSet<BlockPos> posSet = new HashSet<>();
        posSet.add(new BlockPos(this.getTile()));
        for(DirPos pos : conPos) {
            ForgeDirection dir = pos.getDir();
            posSet.add(new BlockPos(pos.getX() - dir.offsetX, pos.getY() - dir.offsetY, pos.getZ() - dir.offsetZ));
        }

        return new FluidNode(type.getNetworkProvider(), posSet.toArray(new BlockPos[posSet.size()])).setConnections(conPos);
    }

    @Override
    protected TickRateModulation doBusWork() {
        if (!this.getProxy().isActive() || !this.canDoBusWork()) {
            return TickRateModulation.IDLE;
        }

        this.didSomething = false;

        try {
            final IMEMonitor<IAEItemStack> inv = this.getProxy().getStorage().getItemInventory();

            if (inv != null) {
                FluidType type = getFluidTypeFromFilterSlot();
                //ItemStack fluidIconStack = new ItemStack(ModItems.fluid_icon, 64, type.getID());



                System.out.println("[export] inv exists" + type.getName());
                System.out.println("[export] fill: " + this.tank.getFill());

                this.tank.setTankType(type);

                if (tank.getFill() >= fluidRate) {
                    int fluidAmount = tank.getFill();
                    int itemsToCreate = fluidAmount / fluidRate;
                    int remainingFluidd = fluidAmount % fluidRate;
                    if (itemsToCreate > 0) {
                        ItemStack fluidItemStack = new ItemStack(ModItems.fluid_icon, itemsToCreate, type.getID());
                        IAEItemStack itemStackToInsert = AEItemStack.create(fluidItemStack);
                        IAEItemStack notInserted = inv.injectItems(itemStackToInsert, Actionable.SIMULATE, mySrc);
                        if (notInserted == null || notInserted.getStackSize() == 0) {
                            inv.injectItems(itemStackToInsert, Actionable.MODULATE, mySrc);
                            this.tank.setFill(remainingFluidd);
                            this.didSomething = true;
                        } else {
                            int itemsInserted = (int) (itemsToCreate - notInserted.getStackSize());
                            if (itemsInserted > 0) {
                                ItemStack insertedStack = new ItemStack(ModItems.fluid_icon, itemsInserted, type.getID());
                                IAEItemStack insertedAEStack = AEItemStack.create(insertedStack);
                                inv.injectItems(insertedAEStack, Actionable.MODULATE, mySrc);
                                tank.setFill(remainingFluidd);
                                this.didSomething = true;
                            }
                        }
                    }
                }

                World worldObj = this.getTile().getWorldObj();
                int xCoord = this.getTile().xCoord;
                int yCoord = this.getTile().yCoord;
                int zCoord = this.getTile().zCoord;

                if (this.node == null || this.node.expired || this.tank.getTankType() != lastType) {
                    this.node = (FluidNode) UniNodespace.getNode(worldObj, xCoord, yCoord, zCoord, tank.getTankType().getNetworkProvider());

                    if(this.node == null || this.node.expired || tank.getTankType() != lastType) {
                        this.node = this.createNode(tank.getTankType());
                        UniNodespace.createNode(worldObj, this.node);
                        lastType = tank.getTankType();
                    }
                }

                if (node != null && node.hasValidNet()) {
                    node.net.addReceiver(this);
                }

                // HBMFluidBridge.pushFromHBMToAE2(inv, this.mySrc, this.tank, fluidIconStack);


            }
        } catch (Exception ignored) { ignored.printStackTrace(); }

        return this.didSomething ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }



    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.ExportBus.getMin(), TickRates.ExportBus.getMax(), false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        return doBusWork();
    }

    // ---------- Rendering ----------
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
                CableBusTextures.PartExportSides.getIcon()
        );

        rh.setBounds(4, 4, 12, 12, 12, 14);
        rh.renderInventoryBox(renderer);

        rh.setBounds(5, 5, 14, 11, 11, 15);
        rh.renderInventoryBox(renderer);

        rh.setBounds(6, 6, 15, 10, 10, 16);
        rh.renderInventoryBox(renderer);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(final int x, final int y, final int z, final IPartRenderHelper rh, final RenderBlocks renderer) {
        this.setRenderCache(rh.useSimplifiedRendering(x, y, z, this, this.getRenderCache()));

        rh.setTexture(
                CableBusTextures.PartExportSides.getIcon(),
                CableBusTextures.PartExportSides.getIcon(),
                CableBusTextures.PartMonitorBack.getIcon(),
                this.getItemStack().getIconIndex(),
                CableBusTextures.PartExportSides.getIcon(),
                CableBusTextures.PartExportSides.getIcon()
        );

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
                CableBusTextures.PartMonitorSidesStatus.getIcon()
        );

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



    // ---------- HBM Fluid Interface ----------
    @Override public FluidTank[] getAllTanks() { return new FluidTank[]{tank}; }
    @Override public long transferFluid(FluidType type, int pressure, long fluid) {

        int toAdd = (int) Math.min(fluid, tank.getMaxFill() - tank.getFill());
        tank.setFill((int)(tank.getFill() + toAdd));


        return fluid - toAdd;
    }
    @Override public long getDemand(FluidType type, int pressure) { return tank.getMaxFill() - tank.getFill(); }
}
