package com.ecstasy.hbmplus.Machines.MachineCanner;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashMap;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.BobMathUtil;
import com.hbm.util.CompatEnergyControl;
import com.hbm.util.I18nUtil;

import api.hbm.tile.IInfoProviderEC;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import api.hbm.energymk2.IBatteryItem;
import api.hbm.energymk2.IEnergyReceiverMK2;
import io.netty.buffer.ByteBuf;
import com.hbm.util.fauxpointtwelve.DirPos;

public class TileEntityMachineCanner extends TileEntityMachineBase implements IEnergyReceiverMK2, IGUIProvider, IUpgradeInfoProvider, IInfoProviderEC {


	public int progress;
	public long power;
	public boolean isProgressing;
	public FluidTank tank;

	private int audioDuration = 0;
	
	private AudioWrapper audio;

	//configurable values
	public static int maxPower = 100000;
	public static int processingSpeed = 200;
	public static int baseConsumption = 200;

	public UpgradeManagerNT upgradeManager = new UpgradeManagerNT();

	private static final int[] slot_io = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };

	public TileEntityMachineCanner() {
		super(9);
	}

	@Override public String getName() {
		return "container.machine_canner";
	}

	@Override public boolean isItemValidForSlot(int i, ItemStack stack) {
		if(i == 28) return stack.getItem() instanceof IBatteryItem;
		if(i < 14) return ShredderRecipes.getShredderResult(stack) != null;

		if (i > 28 && stack.getItem() instanceof ItemMachineUpgrade) {
			ItemMachineUpgrade item = (ItemMachineUpgrade) stack.getItem();
			return canProvideInfo(item.type, item.tier, false);
		}

		return false;
	}

	@Override public int[] getAccessibleSlotsFromSide(int side) {
		return slot_io;
	}

	@Override public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		power = nbt.getLong("power");
		progress = nbt.getShort("progress");
	}

	@Override public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
		nbt.setShort("progress", (short) progress);
	}

	@Override public boolean canInsertItem(int slot, ItemStack itemStack, int side) {
		if((slot >= 14 && slot != 28) || !this.isItemValidForSlot(slot, itemStack))
			return false;
		
		if(slots[slot] == null)
			return true;
		
		int size = slots[slot].stackSize;
		
		for(int k = 0; k < 14; k++) {
			if(slots[k] == null)
				return false;
			
			if(slots[k].getItem() == itemStack.getItem() && slots[k].getItemDamage() == itemStack.getItemDamage() && slots[k].stackSize < size)
				return false;
		}
		
		return true;
	}

	@Override public boolean canExtractItem(int i, ItemStack itemStack, int j) {
		
		if(i > 14 && i < 28)
			return true;
		
		return false;
	}

	public int getCentrifugeProgressScaled(int i) {
		return (progress * i) / processingSpeed;
	}

	public long getPowerRemainingScaled(int i) {
		return (power * i) / maxPower;
	}

	public boolean canProcess() {
		

		for(int i = 0; i < 14; i++)
			{
				if(slots[i] != null && slots[i].stackSize > 0 && hasSpace(slots[i]))
				{
					return true;
				}
			}
		

		return false;
	}

	public void processItem() {
		
		
	}

	public boolean hasSpace(ItemStack stack) {
		
		
		return false;
	}

	public boolean hasPower() {
		return power > 0;
	}

	public boolean isProcessing() {
		return this.progress > 0;
	}

	public DirPos[] getConPos() {
		
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
		
		return new DirPos[] {
				new DirPos(xCoord + dir.offsetX * 2 + rot.offsetX, yCoord, zCoord + dir.offsetZ * 2 + rot.offsetZ, dir),
				new DirPos(xCoord + dir.offsetX * 2 - rot.offsetX, yCoord, zCoord + dir.offsetZ * 2 - rot.offsetZ, dir),
				new DirPos(xCoord - dir.offsetX * 2 + rot.offsetX, yCoord, zCoord - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),
				new DirPos(xCoord - dir.offsetX * 2 - rot.offsetX, yCoord, zCoord - dir.offsetZ * 2 - rot.offsetZ, dir.getOpposite()),
		};
	}

	@Override public void updateEntity() {

		
		if(!worldObj.isRemote) {

			if(worldObj.getTotalWorldTime() % 20 == 0) {
				for(DirPos pos : getConPos()) this.trySubscribe(worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
			}

			power = Library.chargeTEFromItems(slots, 28, power, maxPower);
			
			int consumption = baseConsumption;
			int speed = 1;
			
			upgradeManager.checkSlots(this, slots, 29, 31);
			speed += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
			consumption += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3) * baseConsumption;
			
			speed *= (1 + Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 5);
			consumption += Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * baseConsumption * 50;
			
			consumption /= (1 + Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3));

			unloadItems();

			if(hasPower() && isProcessing()) {
				this.power -= consumption;

				if(this.power < 0) {
					this.power = 0;
				}
			}

			if(hasPower() && canProcess()) {
				isProgressing = true;
			} else {
				isProgressing = false;
			}

			if(isProgressing) {
				progress += speed;

				if(this.progress >= processingSpeed) {
					this.progress = 0;
					this.processItem();
				}
			} else {
				progress = 0;
			}
			
			this.networkPackNT(50);
		} else {
			
			if(isProgressing) {
				audioDuration += 2;
			} else {
				audioDuration -= 3;
			}
			
			audioDuration = MathHelper.clamp_int(audioDuration, 0, 60);
			
			if(audioDuration > 10) {
				
				if(audio == null) {
					audio = createAudioLoop();
					audio.startSound();
				} else if(!audio.isPlaying()) {
					audio = rebootAudio(audio);
				}

				audio.updateVolume(getVolume(1F));
				audio.updatePitch((audioDuration - 10) / 100F + 0.5F);
				
			} else {
				
				if(audio != null) {
					audio.stopSound();
					audio = null;
				}
			}
		}
	}

	@Override public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeInt(progress);
		buf.writeBoolean(isProgressing);
	}
	
	@Override public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		power = buf.readLong();
		progress = buf.readInt();
		isProgressing = buf.readBoolean();
	}

	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
	}

	@Override public AudioWrapper createAudioLoop() {
		return MainRegistry.proxy.getLoopedSound("hbm:block.centrifugeOperate", xCoord, yCoord, zCoord, 1.0F, 10F, 1.0F);
	}

	@Override public void onChunkUnload() {

		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}

	@Override public void invalidate() {

		super.invalidate();

		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}
	
	@Override public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override public void setPower(long i) {
		power = i;
	}

	@Override public long getPower() {
		return power;
	}

	@Override public long getMaxPower() {
		return maxPower;
	}

	@Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerMachineCanner(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachineCanner(player.inventory, this);
	}

	@Override public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
		return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
	}

	@Override public boolean canConnect(ForgeDirection dir) {
		return dir != ForgeDirection.UNKNOWN && dir != ForgeDirection.DOWN;
	}

	@SuppressWarnings("static-access")
	@Override public void provideInfo(UpgradeType type, int level, List<String> info, boolean extendedInfo) {
		info.add(IUpgradeInfoProvider.getStandardLabel(ModBlocks.machine_centrifuge));
		if(type == UpgradeType.SPEED) {
			info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey(this.KEY_DELAY, "-" + (100 - 100 / (level + 1)) + "%"));
			info.add(EnumChatFormatting.RED + I18nUtil.resolveKey(this.KEY_CONSUMPTION, "+" + (level * 100) + "%"));
		}
		if(type == UpgradeType.POWER) {
			info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey(this.KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%"));
		}
		if(type == UpgradeType.OVERDRIVE) {
			info.add((BobMathUtil.getBlink() ? EnumChatFormatting.RED : EnumChatFormatting.DARK_GRAY) + "YES");
		}
	}

	@Override public void setInventorySlotContents(int i, ItemStack stack) {
		slots[i] = stack;
		
		if(stack != null && stack.getItem() instanceof ItemMachineUpgrade && i >= 28) {
			worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "hbm:item.upgradePlug", 1.0F, 1.0F);
		}
	}

	@Override public ItemStack decrStackSize(int i, int j) {
		if(slots[i] != null)
		{
			if(slots[i].stackSize <= j)
			{
				ItemStack itemStack = slots[i];
				slots[i] = null;
				return itemStack;
			}
			ItemStack itemStack1 = slots[i].splitStack(j);
			if (slots[i].stackSize == 0)
			{
				slots[i] = null;
			}
			
			return itemStack1;
		} else {
			return null;
		}
	}

	@Override public void provideExtraInfo(NBTTagCompound data) {
		data.setBoolean(CompatEnergyControl.B_ACTIVE, this.progress > 0);
		data.setInteger(CompatEnergyControl.B_ACTIVE, this.progress);
	}

	public boolean isRunning() {
		return this.isProcessing();
	}

	private void unloadItems() {
		
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset).getOpposite();
		ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);

		int x = xCoord + dir.offsetX * -2 + rot.offsetX * 0;
		int z = zCoord + dir.offsetZ * -2 + rot.offsetZ * 0;
		
		TileEntity te = worldObj.getTileEntity(x, yCoord, z);
		
		
		
	}

	@Override
	public HashMap<UpgradeType, Integer> getValidUpgrades() {
		HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
		upgrades.put(UpgradeType.SPEED, 3);
		upgrades.put(UpgradeType.POWER, 3);
		upgrades.put(UpgradeType.OVERDRIVE, 3);
		return upgrades;
	}
}
