/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Botania Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 *
 * File Created @ [Apr 30, 2015, 3:57:57 PM (GMT)]
 */
package vazkii.botania.common.block.tile.corporea;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.model.animation.CapabilityAnimation;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import vazkii.botania.api.corporea.CorporeaHelper;
import vazkii.botania.api.corporea.ICorporeaRequestor;
import vazkii.botania.api.corporea.ICorporeaSpark;
import vazkii.botania.api.internal.VanillaPacketDispatcher;

public class TileCorporeaCrystalCube extends TileCorporeaBase implements ICorporeaRequestor {

	private static final String TAG_REQUEST_TARGET = "requestTarget";
	private static final String TAG_ITEM_COUNT = "itemCount";

	private static final double LOG_2 = Math.log(2);

	ItemStack requestTarget;
	int itemCount = 0;
	int ticks = 0;
	public int compValue = 0;

	private final IAnimationStateMachine asm;

	public TileCorporeaCrystalCube() {
		if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
			asm = ModelLoaderRegistry.loadASM(new ResourceLocation("botania", "asms/block/crystalcube_cube.json"), ImmutableMap.of());
		} else {
			asm = null;
		}
	}

	@Override
	public void update() {
		++ticks;
		if(ticks % 20 == 0)
			updateCount();
	}

	public void setRequestTarget(ItemStack stack) {
		if(stack != null) {
			ItemStack copy = stack.copy();
			copy.stackSize = 1;
			requestTarget = copy;
			updateCount();
			if(!worldObj.isRemote)
				VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
		}

	}

	public ItemStack getRequestTarget() {
		return requestTarget;
	}

	public int getItemCount() {
		return itemCount;
	}

	public void doRequest(boolean fullStack) {
		if(worldObj.isRemote)
			return;

		ICorporeaSpark spark = getSpark();
		if(spark != null && spark.getMaster() != null && requestTarget != null) {
			int count = fullStack ? requestTarget.getMaxStackSize() : 1;
			doCorporeaRequest(requestTarget, count, spark);
		}
	}

	private void updateCount() {
		if(worldObj.isRemote)
			return;

		int oldCount = itemCount;
		itemCount = 0;
		ICorporeaSpark spark = getSpark();
		if(spark != null && spark.getMaster() != null && requestTarget != null) {
			List<ItemStack> stacks = CorporeaHelper.requestItem(requestTarget, -1, spark, true, false);
			for(ItemStack stack : stacks)
				itemCount += stack.stackSize;
		}

		if(itemCount != oldCount) {
			VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
			onUpdateCount();
		}
	}

	private void onUpdateCount() {
		compValue = getComparatorValue();
		worldObj.updateComparatorOutputLevel(pos, worldObj.getBlockState(pos).getBlock());
	}

	@Override
	public void writePacketNBT(NBTTagCompound par1nbtTagCompound) {
		super.writePacketNBT(par1nbtTagCompound);
		NBTTagCompound cmp = new NBTTagCompound();
		if(requestTarget != null)
			requestTarget.writeToNBT(cmp);
		par1nbtTagCompound.setTag(TAG_REQUEST_TARGET, cmp);
		par1nbtTagCompound.setInteger(TAG_ITEM_COUNT, itemCount);
	}

	@Override
	public void readPacketNBT(NBTTagCompound par1nbtTagCompound) {
		super.readPacketNBT(par1nbtTagCompound);
		NBTTagCompound cmp = par1nbtTagCompound.getCompoundTag(TAG_REQUEST_TARGET);
		requestTarget = ItemStack.loadItemStackFromNBT(cmp);
		itemCount = par1nbtTagCompound.getInteger(TAG_ITEM_COUNT);
	}

	@Override
	public int getSizeInventory() {
		return 1;
	}

	public int getComparatorValue() {
		if(itemCount == 0)
			return 0;
		return Math.min(15, (int) Math.floor(Math.log(itemCount) / LOG_2) + 1);
	}

	@Override
	protected SimpleItemStackHandler createItemHandler() {
		return new SimpleItemStackHandler(this, false);
	}

	@Override
	public void doCorporeaRequest(Object request, int count, ICorporeaSpark spark) {
		if(!(request instanceof ItemStack))
			return;

		List<ItemStack> stacks = CorporeaHelper.requestItem(request, count, spark, true, true);
		spark.onItemsRequested(stacks);
		boolean did = false;
		for(ItemStack reqStack : stacks)
			if(requestTarget != null) {
				EntityItem item = new EntityItem(worldObj, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, reqStack);
				worldObj.spawnEntityInWorld(item);
				itemCount -= reqStack.stackSize;
				did = true;
			}

		if(did) {
			VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
			onUpdateCount();
		}
	}

	@Override
	public boolean hasCapability(@Nonnull Capability<?> cap, @Nonnull EnumFacing side) {
		return cap == CapabilityAnimation.ANIMATION_CAPABILITY || super.hasCapability(cap, side);
	}

	@Nonnull
	@Override
	public <T> T getCapability(@Nonnull Capability<T> cap, @Nonnull EnumFacing side) {
		if(cap == CapabilityAnimation.ANIMATION_CAPABILITY) {
			return CapabilityAnimation.ANIMATION_CAPABILITY.cast(asm);
		} else return super.getCapability(cap, side);
	}

}
