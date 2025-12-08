package com.ecstasy.hbmplus.AE2.FluidExport;

import api.hbm.fluidmk2.FluidNode;
import api.hbm.fluidmk2.IFluidProviderMK2;
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
import com.hbm.items.ModItems;

import com.ecstasy.hbmplus.Integration.HBMFluidBridge;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.IItemFluidIdentifier;

import api.hbm.fluidmk2.IFluidUserMK2;
import api.hbm.fluidmk2.IFluidReceiverMK2;

import com.hbm.uninos.UniNodespace;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import appeng.util.Platform;
import appeng.core.sync.GuiBridge;
import net.minecraft.util.Vec3;
import net.minecraft.entity.player.EntityPlayer;
import com.hbm.items.machine.IItemFluidIdentifier;
import appeng.api.config.Actionable;
import appeng.util.item.AEItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashSet;

import api.hbm.fluidmk2.IFluidProviderMK2;


public class PartFluidExport extends PartSharedItemBus implements IFluidUserMK2, IFluidProviderMK2 {

    private final MachineSource mySrc;
    private final FluidTank tank;
    private static final int FLUID_FILTER_SLOT = 0;

    private static final int fluidRate = 1;

    private boolean didSomething = false;
    protected FluidNode node;
    protected FluidType lastType;

    @Reflected
    public PartFluidExport(ItemStack is) {
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

    private FluidType getFilterType() {
        ItemStack stack = getInventoryByName("config").getStackInSlot(FLUID_FILTER_SLOT);
        if (stack != null && stack.getItem() instanceof IItemFluidIdentifier) {
            return ((IItemFluidIdentifier) stack.getItem()).getType(null, 0, 0, 0, stack);
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
            System.out.println("[import] xd");
            return TickRateModulation.IDLE;
        }

        this.didSomething = false;

        try {
            final IMEMonitor<IAEItemStack> inv = this.getProxy().getStorage().getItemInventory();

            if (inv != null) {
                FluidType filterType = getFilterType();
                if (filterType != Fluids.NONE && (this.tank.getTankType() == Fluids.NONE || this.tank.getTankType() == filterType)) {
                    ItemStack fluidIconStack = new ItemStack(ModItems.fluid_icon, 64, filterType.getID());
                    IAEItemStack itemStackToExtract = AEItemStack.create(fluidIconStack);
                    IAEItemStack extractedStack = inv.extractItems(itemStackToExtract, Actionable.SIMULATE, mySrc);

                    System.out.println("[import] extracted: "+ extractedStack.getStackSize());

                    if (extractedStack.getStackSize() > 0) {
                        int itemsToExtract = (int) extractedStack.getStackSize();
                        int maxFillAmount = this.tank.getMaxFill() - this.tank.getFill();
                        int maxItemsBasedOnTank = maxFillAmount / fluidRate;
                        int finalItemsToExtract = Math.min(itemsToExtract, maxItemsBasedOnTank);
                        System.out.println("[import] poop: "+ finalItemsToExtract);
                        if (finalItemsToExtract > 0) {
                            itemStackToExtract.setStackSize(finalItemsToExtract);
                            IAEItemStack actualExtracted = inv.extractItems(itemStackToExtract, Actionable.MODULATE, mySrc);
                            int fluidAmount = finalItemsToExtract * fluidRate; // Convert to mB
                            this.tank.setTankType(filterType);
                            this.tank.setFill(this.tank.getFill() + fluidAmount);
                            this.didSomething = true;
                        }
                    }

                    System.out.println("[import] " + this.tank.getTankType().getName());
                    System.out.println("[import] fill: " + this.tank.getFill());

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
                        node.net.addProvider(this);
                    }
                }

                //HBMFluidBridge.pullFromAE2ToHBM(inv, this.mySrc, this.tank);
            }
        } catch (Exception ignored) { ignored.printStackTrace(); }

        return this.didSomething ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }



    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.ImportBus.getMin(), TickRates.ImportBus.getMax(), false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        return doBusWork();
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
    @Override public void useUpFluid(FluidType type, int pressure, long amount) { tank.setFill((int)((long) tank.getFill() - amount)); }
    @Override public long getFluidAvailable(FluidType type, int pressure) { return tank.getFill(); }
}
