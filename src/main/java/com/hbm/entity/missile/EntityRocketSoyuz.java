package com.hbm.entity.missile;

import java.util.List;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.main.AdvancementManager;
import com.hbm.interfaces.AutoRegister;
import com.hbm.items.ModItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.ModDamageSource;
import com.hbm.main.MainRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

@AutoRegister(name = "entity_soyuz_new", trackingRange = 1000)
public class EntityRocketSoyuz extends EntityRocketBase {
    public static final DataParameter<Integer> SKIN = EntityDataManager.createKey(EntityRocketSoyuz.class, DataSerializers.VARINT);
    public int mode;
    public int targetX;
    public int targetZ;
    private boolean memed;

    public EntityRocketSoyuz(World world) {
        super(world, 18);
        setSize(5F, 50F);
    }

    @Override public void onUpdate() {
        super.onUpdate();
        if(isDead) return;
        if(!world.isRemote) {
            List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(this, new AxisAlignedBB(posX - 5, posY - 15, posZ - 5, posX + 5, posY, posZ + 5));
            for(Entity entity : entities) {
                entity.setFire(15);
                entity.attackEntityFrom(ModDamageSource.exhaust, 100F);
                if(entity instanceof EntityPlayer) {
                    if(!memed) {
                        memed = true;
                        world.playSound(null, posX, posY, posZ, HBMSoundHandler.soyuzed, SoundCategory.NEUTRAL, 100, 1F);
                    }
                    AdvancementManager.grantAchievement((EntityPlayer) entity, AdvancementManager.achSoyuz);
                }
            }
        } else {
            spawnExhaust(posX, posZ);
            spawnExhaust(posX + 2.75, posZ);
            spawnExhaust(posX - 2.75, posZ);
            spawnExhaust(posX, posZ + 2.75);
            spawnExhaust(posX, posZ - 2.75);
        }
    }

    private void spawnExhaust(double x, double z) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("type", "exhaust");
        data.setString("mode", "soyuz");
        data.setInteger("count", 1);
        data.setDouble("width", world.rand.nextDouble() * .25 - .5);
        data.setDouble("posX", x); data.setDouble("posY", posY); data.setDouble("posZ", z);
        MainRegistry.proxy.effectNT(data);
    }

    public void setPayload(List<ItemStack> items) {
        for(int i = 0; i < payload.length; i++) payload[i] = i < items.size() ? items.get(i).copy() : ItemStack.EMPTY;
    }

    @Override protected void deployPayload() {
        if(mode == 0) {
            ItemStack load = payload[0];
            if(!load.isEmpty() && load.getItem() == ModItems.flame_pony) {
                ExplosionLarge.spawnTracers(world, posX, posY, posZ, 25);
                for(EntityPlayer player : world.playerEntities) AdvancementManager.grantAchievement(player, AdvancementManager.achSpace);
                payload[0] = ItemStack.EMPTY;
            } else {
                if(!load.isEmpty() && load.getItem() == ModItems.sat_foeq)
                    for(EntityPlayer player : world.playerEntities) AdvancementManager.grantAchievement(player, AdvancementManager.achFOEQ);
                super.deployPayload();
            }
        } else if(mode == 1) {
            EntitySoyuzCapsule capsule = new EntitySoyuzCapsule(world);
            for(int i = 0; i < payload.length; i++) capsule.payload[i] = payload[i].copy();
            capsule.soyuz = getSkin();
            capsule.setPosition(targetX + .5, 600, targetZ + .5);
            world.getChunkProvider().provideChunk(targetX >> 4, targetZ >> 4);
            if(world.spawnEntity(capsule)) {
                java.util.Arrays.fill(payload, ItemStack.EMPTY);
            } else {
                for(ItemStack stack : payload) if(!stack.isEmpty()) entityDropItem(stack.copy(), 0F);
                entityDropItem(new ItemStack(ModItems.missile_soyuz, 1, getSkin()), 0F);
            }
        }
    }

    @Override protected void entityInit() { dataManager.register(SKIN, 0); }
    public void setSkin(int skin) { dataManager.set(SKIN, skin); }
    public int getSkin() { return dataManager.get(SKIN); }
    @Override protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        setSkin(nbt.getInteger("skin")); mode = nbt.getInteger("mode");
        targetX = nbt.getInteger("targetX"); targetZ = nbt.getInteger("targetZ");
    }
    @Override protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("skin", getSkin()); nbt.setInteger("mode", mode);
        nbt.setInteger("targetX", targetX); nbt.setInteger("targetZ", targetZ);
    }
}
