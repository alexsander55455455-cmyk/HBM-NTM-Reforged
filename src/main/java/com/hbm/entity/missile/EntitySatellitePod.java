package com.hbm.entity.missile;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.tileentity.machine.TileEntityMachineSatDock;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.hbm.interfaces.AutoRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import java.util.Arrays;
import com.hbm.util.ParticleUtil;

import com.hbm.api.entity.IRadarDetectableNT;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

@AutoRegister(name = "entity_satellite_pod", trackingRange = 1000)
public class EntitySatellitePod extends EntityThrowableInterp implements IRadarDetectableNT {
	
	public ItemStack[] slots = new ItemStack[0];
	
	// wait timer for when the pod lands and unloads
	public int timer = 0;
	
	// y height of the pad that has called this pod, used for braking in time
	public int callerYPos;
	
	public double speed = 0.75D;
	
	public static final DataParameter<Integer> STATE = EntityDataManager.createKey(EntitySatellitePod.class, DataSerializers.VARINT);
	public static final int STATE_LEGS_UP = 0;
	public static final int STATE_LEGS_DOWN = 1;

	public float legs = 0F;
	public float prevLegs = 0F;
	public static final float LEG_SPEED = 1F / 20F; // 1 second

	public EntitySatellitePod(World world) {
		super(world);
		this.ignoreFrustumCheck = true;
		this.isImmuneToFire = true;
		this.setSize(0.95F, 5.25F);
	}
	
	public EntitySatellitePod setup(int caller, ItemStack... cargo) {
		this.callerYPos = caller;
		this.slots = new ItemStack[cargo.length];
        for(int i = 0; i < cargo.length; i++) this.slots[i] = cargo[i].copy();
		return this;
	}

	@Override
	protected void entityInit() {
		super.entityInit();
        this.dataManager.register(STATE, STATE_LEGS_UP);
	}
	
	public boolean doesDeployLegs() {
		return this.dataManager.get(STATE) == STATE_LEGS_DOWN;
	}
	
	public void setDeployLegs(boolean deploy) {
		this.dataManager.set(STATE, deploy ? STATE_LEGS_DOWN : STATE_LEGS_UP);
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if(this.isEntityInvulnerable(source)) {
			return false;
		} else {
			if(amount >= 5F && !this.world.isRemote && !this.isDead) {
				this.setDead();
				
				ExplosionVNT xnt = new ExplosionVNT(world, posX, posY + 1.5, posZ, 15F);
				xnt.setEntityProcessor(new EntityProcessorCrossSmooth(1D, 50));
				xnt.setPlayerProcessor(new PlayerProcessorStandard());
				xnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
				xnt.explode();
				
				if(this.motionY != 0) ExplosionLarge.spawnShrapnelShower(world, posX, posY + 1.5, posZ, motionX, motionY, motionZ, 15, 0.25);
			}

			return true;
		}
	}
	
	@Override
	public void onUpdate() {
		
		if(!world.isRemote) {
			
			if(this.isLanding()) {
				
				// unloading
				if(this.timer > 0) {
					this.timer++;
					
					this.posY = Math.ceil(posY); // hack hack hackedy hack
					
					if(this.timer >= 100) {
						this.unloadItems();
					}
					
				// just landed
				} else if(this.onGround) {
					this.speed = 0D;
					this.timer = 1;
					this.setDeployLegs(true);
					
				// still descending
				} else {
					if(this.posY < this.callerYPos + 17 && !this.doesDeployLegs()) this.setDeployLegs(true);
					if(this.posY < this.callerYPos + 25) this.speed -= 0.01;
					this.speed = MathHelper.clamp(this.speed, 0.025D, 0.75D);
				}
				
				this.motionY = -this.speed;
				
			} else {
				
				this.onGround = false;

				this.speed += 0.01;
				if(this.speed >= 0.2) this.setDeployLegs(false);
				this.speed = MathHelper.clamp(this.speed, 0D, 2D);
				this.motionY = this.speed;
				
				if(this.posY > 300) this.setDead();
			}
			
		} else {
			
			this.prevLegs = this.legs;
			
			if(doesDeployLegs()) {
				this.legs += LEG_SPEED;
			} else {
				this.legs -= LEG_SPEED;
			}
			
			this.legs = MathHelper.clamp(this.legs, 0F, 1F);
			
			if(this.legs > 0 && this.motionY < 0 || this.motionY > 0) {
				ParticleUtil.spawnGasFlame(world, posX, posY + 0.5, posZ, 0, this.speed - 1, 0);
			}
		}
		
		super.onUpdate();
	}
	
	/** Tries to fill a sat dock that this pod is standing on. All items that cannot be added to a dock will be spilled and removed from the pod's inventory. */
	public void unloadItems() {
        if(world.isRemote) return;

		int x = (int) Math.floor(posX);
		int y = (int) Math.floor(posY - 0.5);
		int z = (int) Math.floor(posZ);
		
        BlockPos dockPos = new BlockPos(x, y, z);
        if(world.getBlockState(dockPos).getBlock() == ModBlocks.sat_dock) {
            TileEntity tile = world.getTileEntity(dockPos);
            if(tile instanceof TileEntityMachineSatDock) {
                TileEntityMachineSatDock dock = (TileEntityMachineSatDock) tile;
                for(int i = 0; i < slots.length; i++) {
                    for(int slot = 0; slot < 15 && !slots[i].isEmpty(); slot++) {
                        slots[i] = dock.inventory.insertItem(slot, slots[i], false);
                    }
                }
            }
        }
        boolean remaining = false;
        for(int i = 0; i < slots.length; i++) {
            if(slots[i].isEmpty()) continue;
            EntityItem drop = new EntityItem(world, posX, posY + 0.25, posZ, slots[i].copy());
            drop.setDefaultPickupDelay();
            if(world.spawnEntity(drop)) slots[i] = ItemStack.EMPTY;
            else remaining = true;
        }
        // Retry cancelled item spawns without losing cargo or duplicating dock insertions.
        if(!remaining) slots = new ItemStack[0];
    }
    
	public boolean isLanding() {
		return this.slots.length > 0;
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		dataManager.set(STATE, nbt.getInteger("state"));
		timer = nbt.getInteger("timer");
		callerYPos = nbt.getInteger("callerYPos");
		speed = nbt.getDouble("speed");

		int itemCount = Math.max(0, Math.min(256, nbt.getInteger("itemCount")));
		NBTTagList items = nbt.getTagList("items", 10);
		
		this.slots = new ItemStack[itemCount];
        Arrays.fill(slots, ItemStack.EMPTY);

		for(int i = 0; i < items.tagCount(); i++) {
			
			NBTTagCompound itemTag = items.getCompoundTagAt(i);
			int j = itemTag.getByte("slot") & 255;

			if(j >= 0 && j < this.slots.length) {
				this.slots[j] = new ItemStack(itemTag);
			}
		}
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setInteger("state", dataManager.get(STATE));
		nbt.setInteger("timer", timer);
		nbt.setInteger("callerYPos", callerYPos);
		nbt.setDouble("speed", speed);

		nbt.setInteger("itemCount", this.slots.length);
		NBTTagList items = new NBTTagList();

		for(int i = 0; i < this.slots.length; i++) {
			
			if(!this.slots[i].isEmpty()) {
				NBTTagCompound itemTag = new NBTTagCompound();
				itemTag.setByte("slot", (byte) i);
				this.slots[i].writeToNBT(itemTag);
				items.appendTag(itemTag);
			}
		}

		nbt.setTag("items", items);
	}

	@Override
	protected void onImpact(RayTraceResult mop) {
		
		if(mop.typeOfHit == RayTraceResult.Type.BLOCK) {
			this.setPosition(mop.hitVec.x, mop.hitVec.y, mop.hitVec.z);
			this.onGround = true;
			
            // The projectile base still applies motion after onImpact.
            this.speed = 0;
            this.motionY = 0;
		}
	}
	
	@Override protected boolean canTriggerWalking() { return false; }
	@Override public boolean doesImpactEntities() { return false; }
	
	@Override protected float getAirDrag() { return 1F; }
	@Override protected float getWaterDrag() { return 1F; }
	@Override public float getGravityVelocity() { return 0F; }

	@Override public String getTranslationKey() { return "radar.target.dropship"; }
	@Override public int getBlipLevel() { return IRadarDetectableNT.SPECIAL; }
	@Override public boolean canBeSeenBy(Object radar) { return !(radar instanceof TileEntityTurretBaseNT); }
	@Override public boolean paramsApplicable(RadarScanParams params) { return params.scanMissiles; }
	@Override public boolean suppliesRedstone(RadarScanParams params) { return false; }
}
