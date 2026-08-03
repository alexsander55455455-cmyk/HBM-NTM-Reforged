package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockFlammable;
import com.hbm.blocks.generic.BlockMeta;
import com.hbm.render.block.BlockBakeFrame;
import com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2;
import com.hbm.tileentity.machine.pile.TileEntityPileCore;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class BlockPileBrick extends BlockFlammable implements IToolable {

    public BlockPileBrick(String name) {
        super(Material.ROCK, name, 30, 5, BlockBakeFrame.cube(
                "pile_brick_top", "pile_brick_top", "pile_brick",
                "pile_brick", "pile_brick_side", "pile_brick_side"));
    }

    @Override
    public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side,
                           float hitX, float hitY, float hitZ, EnumHand hand, ToolType tool) {
        if (tool != ToolType.HAND_DRILL || side.getAxis() == EnumFacing.Axis.Y) return false;
        if (world.isRemote) return true;

        BlockPos origin = new BlockPos(x, y, z);
        EnumFacing inward = side.getOpposite();
        EnumFacing leftDirection = inward.rotateYCCW();

        int positiveHeight = probe(world, origin, EnumFacing.UP, BlockPile.MAX_V_SIZE);
        int negativeHeight = probe(world, origin, EnumFacing.DOWN, BlockPile.MAX_V_SIZE);
        int left = probe(world, origin, leftDirection, BlockPile.MAX_H_SIZE);
        int right = probe(world, origin, leftDirection.getOpposite(), BlockPile.MAX_H_SIZE);
        int depthOffset = probe(world, origin, inward, BlockPile.MAX_H_SIZE);

        if (!isSpanWithinLimit(positiveHeight, negativeHeight, BlockPile.MAX_V_SIZE)) {
            sendError(player, origin, "Height too high (>" + BlockPile.MAX_V_SIZE + ")");
            return true;
        }
        if (!isSpanWithinLimit(left, right, BlockPile.MAX_H_SIZE)) {
            sendError(player, origin, "Width too high (>" + BlockPile.MAX_H_SIZE + ")");
            return true;
        }
        if (depthOffset + 1 > BlockPile.MAX_H_SIZE) {
            sendError(player, origin, "Depth too high (>" + BlockPile.MAX_H_SIZE + ")");
            return true;
        }

        if (positiveHeight + negativeHeight + 1 < BlockPile.MIN_V_SIZE) {
            sendError(player, origin, "Height too low (<" + BlockPile.MIN_V_SIZE + ")");
            return true;
        }
        if (left + right + 1 < BlockPile.MIN_H_SIZE) {
            sendError(player, origin, "Width too low (<" + BlockPile.MIN_H_SIZE + ")");
            return true;
        }
        if (depthOffset + 1 < BlockPile.MIN_H_SIZE) {
            sendError(player, origin, "Depth too low (<" + BlockPile.MIN_H_SIZE + ")");
            return true;
        }
        if (positiveHeight == 0 || negativeHeight == 0 || left == 0 || right == 0) {
            sendError(player, origin, "Core cannot be on an edge");
            return true;
        }

        BlockPos min = origin;
        BlockPos max = origin;
        for (int vertical = -negativeHeight; vertical <= positiveHeight; vertical++) {
            for (int horizontal = -left; horizontal <= right; horizontal++) {
                for (int depth = 0; depth <= depthOffset; depth++) {
                    BlockPos target = origin.up(vertical)
                            .offset(leftDirection.getOpposite(), horizontal)
                            .offset(inward, depth);
                    if (world.getBlockState(target).getBlock() != this) {
                        sendError(player, target, "Graphite block missing");
                        return true;
                    }
                    min = new BlockPos(Math.min(min.getX(), target.getX()), Math.min(min.getY(), target.getY()),
                            Math.min(min.getZ(), target.getZ()));
                    max = new BlockPos(Math.max(max.getX(), target.getX()), Math.max(max.getY(), target.getY()),
                            Math.max(max.getZ(), target.getZ()));
                }
            }
        }

        for (int vertical = -negativeHeight; vertical <= positiveHeight; vertical++) {
            for (int horizontal = -left; horizontal <= right; horizontal++) {
                for (int depth = 0; depth <= depthOffset; depth++) {
                    BlockPos target = origin.up(vertical)
                            .offset(leftDirection.getOpposite(), horizontal)
                            .offset(inward, depth);
                    int meta;
                    if (target.equals(origin)) {
                        meta = BlockPile.META_CORE;
                    } else {
                        int edgeCount = 0;
                        if (vertical == -negativeHeight || vertical == positiveHeight) edgeCount++;
                        if (horizontal == -left || horizontal == right) edgeCount++;
                        if (depth == 0 || depth == depthOffset) edgeCount++;
                        meta = edgeCount > 1 ? BlockPile.META_EDGE : BlockPile.META_DUMMY;
                    }
                    world.setBlockState(target,
                            ModBlocks.pile_block.getDefaultState().withProperty(BlockMeta.META, meta), 2);
                    TileEntity tile = world.getTileEntity(target);
                    if (meta == BlockPile.META_CORE) {
                        if (!(tile instanceof TileEntityPileCore)) {
                            tile = new TileEntityPileCore();
                            world.setTileEntity(target, tile);
                        }
                    } else {
                        if (!(tile instanceof TileEntityPileBaseMK2)) {
                            tile = new TileEntityPileBaseMK2();
                            world.setTileEntity(target, tile);
                        }
                        ((TileEntityPileBaseMK2) tile).setCore(origin);
                    }
                }
            }
        }

        TileEntity tile = world.getTileEntity(origin);
        if (tile instanceof TileEntityPileCore) {
            ((TileEntityPileCore) tile).setupSize(positiveHeight, negativeHeight, left, right,
                    depthOffset + 1, inward, min, max);
        }
        world.notifyNeighborsOfStateChange(origin, ModBlocks.pile_block, false);
        return true;
    }

    private int probe(World world, BlockPos origin, EnumFacing direction, int limit) {
        int result = 0;
        for (int i = 1; i <= limit; i++) {
            if (world.getBlockState(origin.offset(direction, i)).getBlock() != this) break;
            result = i;
        }
        return result;
    }

    static boolean isSpanWithinLimit(int positive, int negative, int maximum) {
        return positive >= 0 && negative >= 0 && maximum > 0
                && positive + negative + 1 <= maximum;
    }

    private void sendError(EntityPlayer player, BlockPos pos, String message) {
        player.sendMessage(new TextComponentString("[Chicago Pile " + pos.getX() + ", " + pos.getY() + ", " +
                pos.getZ() + "] " + message).setStyle(new Style().setColor(TextFormatting.RED)));
    }
}
