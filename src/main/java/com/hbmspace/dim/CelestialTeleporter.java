package com.hbmspace.dim;

import com.hbmspace.config.SpaceConfig;
import com.hbmspace.entity.missile.EntityRideableRocket;
import com.hbmspace.main.SpaceMain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class CelestialTeleporter extends Teleporter {

	private final WorldServer sourceServer;
	private final WorldServer targetServer;

	private final double x;
	private double y;
	private final double z;

	private final boolean grounded;

	private final Entity entity;
	private final UUID queueKey;

	public CelestialTeleporter(WorldServer sourceServer, WorldServer targetServer, Entity entity, double x, double y, double z, boolean grounded) {
		this(sourceServer, targetServer, entity, x, y, z, grounded, entity.getUniqueID());
	}

	public CelestialTeleporter(WorldServer sourceServer, WorldServer targetServer, Entity entity, double x, double y, double z, boolean grounded, UUID queueKey) {
		super(targetServer);
		this.sourceServer = sourceServer;
		this.targetServer = targetServer;
		this.entity = entity;
		this.x = x;
		this.y = y;
		this.z = z;
		this.grounded = grounded;
		this.queueKey = queueKey;
	}

	@Override
	public void placeInPortal(@NotNull Entity entityIn, float rotationYaw) {
		prepareDestination();
		entityIn.setPosition(this.x, this.y, this.z);
	}

	private void prepareDestination() {
		int ix = MathHelper.floor(this.x);
		int iz = MathHelper.floor(this.z);

		if (grounded) {
			BlockPos top = targetServer.getTopSolidOrLiquidBlock(new BlockPos(ix, 0, iz));
			this.y = top.getY() + 5;
		} else {
			int cx = ix >> 4;
			int cz = iz >> 4;
			targetServer.getChunkProvider().provideChunk(cx, cz);
		}
	}

	private void positionTransferredEntity(Entity transferredEntity) {
		prepareDestination();
		transferredEntity.setPosition(this.x, this.y, this.z);
		targetServer.updateEntityWithOptionalForce(transferredEntity, false);
	}

	private void runTeleport() {
		MinecraftServer mcServer = FMLCommonHandler.instance().getMinecraftServerInstance();
		if (mcServer == null || entity.isDead || entity.world != sourceServer || entity.dimension != sourceServer.provider.getDimension()) {
			logSkippedTransfer("source entity is no longer available in the queued dimension");
			return;
		}
		PlayerList manager = mcServer.getPlayerList();

		Entity transferEntity = entity;
		if (!entity.getPassengers().isEmpty() && entity.getPassengers().getFirst() instanceof EntityPlayerMP) {
			transferEntity = entity.getPassengers().getFirst();
		}

		if (transferEntity instanceof EntityPlayerMP playerMP) {
			transferPlayer(manager, playerMP);
		} else {
			Entity transferredEntity = transferEntity.changeDimension(targetServer.provider.getDimension(), this);
			if (transferredEntity != null) {
				positionTransferredEntity(transferredEntity);
			}
		}
	}

	private void transferPlayer(PlayerList manager, EntityPlayerMP player) {
		Entity oldMount = player.getRidingEntity();
		if (oldMount == null || oldMount.isDead) {
			manager.transferPlayerToDimension(player, targetServer.provider.getDimension(), this);
			player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
			return;
		}
		if (oldMount.world != sourceServer || oldMount.dimension != sourceServer.provider.getDimension()) {
			SpaceMain.logger.error("[RocketTransfer] Refusing transfer: passenger {} and mount UUID {} are not in the same source dimension {}",
					player.getName(), oldMount.getUniqueID(), sourceServer.provider.getDimension());
			return;
		}

		UUID oldMountUuid = oldMount.getUniqueID();
		Entity existingTargetEntity = targetServer.getEntityFromUuid(oldMountUuid);
		if (existingTargetEntity != null) {
			SpaceMain.logger.error("[RocketTransfer] Refusing duplicate transfer: target dimension {} already contains {} with UUID {}",
					targetServer.provider.getDimension(), existingTargetEntity.getClass().getSimpleName(), oldMountUuid);
			return;
		}

		double sourceX = oldMount.posX;
		double sourceY = oldMount.posY;
		double sourceZ = oldMount.posZ;
		String rocketState = oldMount instanceof EntityRideableRocket rocket ? rocket.getState().name() : "not-a-rocket";

		player.dismountRidingEntity();
		manager.transferPlayerToDimension(player, targetServer.provider.getDimension(), this);

		Entity newMount = oldMount.changeDimension(targetServer.provider.getDimension(), this);
		if (newMount == null || targetServer.getEntityFromUuid(newMount.getUniqueID()) != newMount) {
			SpaceMain.logger.error("[RocketTransfer] Mount transfer failed from dimension {} to {} for UUID {}",
					sourceServer.provider.getDimension(), targetServer.provider.getDimension(), oldMountUuid);
			return;
		}
		positionTransferredEntity(newMount);

		if (newMount instanceof EntityRideableRocket rocket) {
			rocket.setThrower(player);
		}
		boolean mounted = player.startRiding(newMount, true);
		if (mounted && newMount instanceof EntityRideableRocket rocket) {
			rocket.updatePassenger(player);
		} else if (!mounted) {
			SpaceMain.logger.error("[RocketTransfer] Passenger {} failed to remount entity UUID {} in dimension {}",
					player.getName(), newMount.getUniqueID(), targetServer.provider.getDimension());
		}
		player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);

		if (SpaceConfig.debugRocketTransfer) {
			SpaceMain.logger.info("[RocketTransfer] sourceDim={} targetDim={} oldRocketUuid={} newRocketUuid={} state={} " +
					"sourcePos=({}, {}, {}) requestedTarget=({}, {}, {}) targetPos=({}, {}, {}) passenger={} passengerUuid={} mounted={}",
					sourceServer.provider.getDimension(), targetServer.provider.getDimension(), oldMountUuid, newMount.getUniqueID(), rocketState,
					sourceX, sourceY, sourceZ, x, y, z, newMount.posX, newMount.posY, newMount.posZ,
					player.getName(), player.getUniqueID(), mounted);
		}
	}

	private void logSkippedTransfer(String reason) {
		if (SpaceConfig.debugRocketTransfer) {
			SpaceMain.logger.info("[RocketTransfer] skipped sourceDim={} targetDim={} entityUuid={} reason={}",
					sourceServer.provider.getDimension(), targetServer.provider.getDimension(), entity.getUniqueID(), reason);
		}
	}

	public static void runQueuedTeleport() {
		CelestialTeleporter teleporter = queue.poll();
		if (teleporter == null) return;
		try {
			teleporter.runTeleport();
		} finally {
			queuedEntities.remove(teleporter.queueKey);
		}
	}

	private static final Queue<CelestialTeleporter> queue = new ArrayDeque<>();
	private static final Set<UUID> queuedEntities = new HashSet<>();

	public static void teleport(Entity entity, int dim, double x, double y, double z, boolean grounded) {
		if (entity.dimension == dim) return; // ignore if we're teleporting to the same place

		MinecraftServer mcServer = FMLCommonHandler.instance().getMinecraftServerInstance();
		Side sidex = FMLCommonHandler.instance().getEffectiveSide();
		if (sidex == Side.SERVER && mcServer != null) {
			WorldServer sourceServer = mcServer.getWorld(entity.dimension);
			WorldServer targetServer = mcServer.getWorld(dim);
			if (sourceServer == null || targetServer == null) {
				SpaceMain.logger.error("[RocketTransfer] Cannot queue teleport from dimension {} to unavailable dimension {}", entity.dimension, dim);
				return;
			}

			Entity queueIdentity = entity.getRidingEntity() != null ? entity.getRidingEntity() : entity;
			UUID queueKey = queueIdentity.getUniqueID();
			if (!queuedEntities.add(queueKey)) {
				if (SpaceConfig.debugRocketTransfer) {
					SpaceMain.logger.info("[RocketTransfer] duplicate queue request ignored sourceDim={} targetDim={} entityUuid={}",
							entity.dimension, dim, queueKey);
				}
				return;
			}

			queue.add(new CelestialTeleporter(sourceServer, targetServer, entity, x, y, z, grounded, queueKey));
		}
	}

}
