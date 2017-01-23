package binnie.extratrees.machines.brewery;

import forestry.api.core.INbtReadable;
import forestry.api.core.INbtWritable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

public class BreweryCrafting implements INbtWritable, INbtReadable {
	@Nullable
	public FluidStack inputFluid;
	public ItemStack[] inputGrains;
	@Nullable
	public ItemStack ingredient;
	@Nullable
	public ItemStack yeast;

	public BreweryCrafting(@Nullable final FluidStack inputFluid, @Nullable final ItemStack ingredient, @Nullable final ItemStack[] inputGrains, final @Nullable ItemStack yeast) {
		this.inputFluid = inputFluid;
		this.inputGrains = ((inputGrains == null) ? new ItemStack[3] : inputGrains);
		this.ingredient = ingredient;
		this.yeast = yeast;
	}

	public BreweryCrafting(final NBTTagCompound nbt) {
		this.readFromNBT(nbt);
	}

	@Override
	public void readFromNBT(final NBTTagCompound nbt) {
		this.inputFluid = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("fluid"));
		this.ingredient = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("ingr"));
		this.inputGrains = new ItemStack[] {
				ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("in1")),
				ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("in2")),
				ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("in3"))
		};
		this.yeast = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("yeast"));
	}

	@Override
	public NBTTagCompound writeToNBT(final NBTTagCompound nbt) {
		if (this.inputFluid != null) {
			final NBTTagCompound fluidTag = new NBTTagCompound();
			this.inputFluid.writeToNBT(fluidTag);
			nbt.setTag("fluid", fluidTag);
		}
		nbt.setTag("ingr", this.getNBT(this.ingredient));
		nbt.setTag("in1", this.getNBT(this.inputGrains[0]));
		nbt.setTag("in2", this.getNBT(this.inputGrains[1]));
		nbt.setTag("in3", this.getNBT(this.inputGrains[2]));
		nbt.setTag("yeast", this.getNBT(this.yeast));
		return nbt;
	}

	private NBTTagCompound getNBT(final ItemStack ingr) {
		if (ingr == null) {
			return new NBTTagCompound();
		}
		final NBTTagCompound nbt = new NBTTagCompound();
		ingr.writeToNBT(nbt);
		return nbt;
	}
}
