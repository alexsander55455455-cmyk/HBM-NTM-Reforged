package com.hbm.entity.missile;

import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.SatelliteTypeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

/** Single-use carrier for the separate Lambda launchpad. Existing Soyuz remains independent. */
public abstract class EntityRocketBase extends Entity {
    protected final ItemStack[] payload;
    protected double acceleration;
    protected EntityRocketBase(World world, int capacity) {
        super(world); ignoreFrustumCheck = true; payload = new ItemStack[capacity];
        java.util.Arrays.fill(payload, ItemStack.EMPTY);
    }
    @Override protected void entityInit() { }
    @Override public void onUpdate() {
        if(motionY < 2) { acceleration += .00025; motionY += acceleration; }
        prevPosX = lastTickPosX = posX; prevPosY = lastTickPosY = posY; prevPosZ = lastTickPosZ = posZ;
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        if(!world.isRemote && posY > 600) { deployPayload(); setDead(); }
    }
    public void setSat(ItemStack stack) { payload[0] = stack.copy(); }
    public ItemStack getSat() { return payload[0].copy(); }
    protected void deployPayload() {
        ItemStack stack = payload[0];
        if(!stack.isEmpty() && SatelliteTypeRegistry.isPayload(stack)) {
            if(SatelliteTypeRegistry.orbit(world, stack, ISatChip.getFreqS(stack), posX, posY, posZ, null).isSuccess()) payload[0] = ItemStack.EMPTY;
            else entityDropItem(stack.copy(), 0F);
        }
    }
    @Override protected void readEntityFromNBT(NBTTagCompound nbt) {
        acceleration = nbt.getDouble("acceleration");
        java.util.Arrays.fill(payload, ItemStack.EMPTY);
        NBTTagList list = nbt.getTagList("items", 10);
        for(int i = 0; i < list.tagCount(); i++) { NBTTagCompound tag = list.getCompoundTagAt(i); int slot = tag.getByte("slot"); if(slot >= 0 && slot < payload.length) payload[slot] = new ItemStack(tag); }
    }
    @Override protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setDouble("acceleration", acceleration); NBTTagList list = new NBTTagList();
        for(int i = 0; i < payload.length; i++) if(!payload[i].isEmpty()) { NBTTagCompound tag = payload[i].writeToNBT(new NBTTagCompound()); tag.setByte("slot", (byte)i); list.appendTag(tag); }
        nbt.setTag("items", list);
    }
    @Override @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) { return distance < 500000; }
}
