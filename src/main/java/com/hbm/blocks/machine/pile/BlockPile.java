package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockMeta;
import com.hbm.render.model.BlockPileConnectedModel;
import com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2;
import com.hbm.tileentity.machine.pile.TileEntityPileCore;
import com.hbm.util.UnlistedPropertyInteger;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class BlockPile extends BlockMeta implements IToolable, ILookOverlay {

    public static final IUnlistedProperty<Integer> MASK_DOWN = new UnlistedPropertyInteger("ct_down");
    public static final IUnlistedProperty<Integer> MASK_UP = new UnlistedPropertyInteger("ct_up");
    public static final IUnlistedProperty<Integer> MASK_NORTH = new UnlistedPropertyInteger("ct_north");
    public static final IUnlistedProperty<Integer> MASK_SOUTH = new UnlistedPropertyInteger("ct_south");
    public static final IUnlistedProperty<Integer> MASK_WEST = new UnlistedPropertyInteger("ct_west");
    public static final IUnlistedProperty<Integer> MASK_EAST = new UnlistedPropertyInteger("ct_east");
    @SuppressWarnings("unchecked")
    public static final IUnlistedProperty<Integer>[] FACE_MASKS = new IUnlistedProperty[]{
            MASK_DOWN, MASK_UP, MASK_NORTH, MASK_SOUTH, MASK_WEST, MASK_EAST
    };

    public static final int MIN_V_SIZE = 5;
    public static final int MIN_H_SIZE = 5;
    public static final int MAX_V_SIZE = 15;
    public static final int MAX_H_SIZE = 15;

    public static final int META_DUMMY = 0;
    public static final int META_CORE = 1;
    public static final int META_CHANNEL = 2;
    public static final int META_FUEL_IN = 3;
    public static final int META_FUEL_OUT = 4;
    public static final int META_AIR_IN = 5;
    public static final int META_AIR_OUT = 6;
    public static final int META_CONTROL = 7;
    public static final int META_EDGE = 8;

    private static final ThreadLocal<Boolean> RESTORING = ThreadLocal.withInitial(() -> false);

    @SideOnly(Side.CLIENT)
    private TextureAtlasSprite sideBase;
    @SideOnly(Side.CLIENT)
    private TextureAtlasSprite topBase;
    @SideOnly(Side.CLIENT)
    private TextureAtlasSprite sideConnected;
    @SideOnly(Side.CLIENT)
    private TextureAtlasSprite topConnected;
    @SideOnly(Side.CLIENT)
    private TextureAtlasSprite inputConnected;
    @SideOnly(Side.CLIENT)
    private TextureAtlasSprite outputConnected;
    @SideOnly(Side.CLIENT)
    private TextureAtlasSprite controlConnected;
    @SideOnly(Side.CLIENT)
    private TextureAtlasSprite coreConnected;

    public BlockPile(String name) {
        super(Material.IRON, name, (short) 9, false);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, new IProperty<?>[]{META}, FACE_MASKS);
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (!(state instanceof IExtendedBlockState extended) || state.getBlock() != this) return state;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (EnumFacing face : EnumFacing.VALUES) {
            extended = extended.withProperty(FACE_MASKS[face.getIndex()], connectionMask(world, pos, face, cursor));
        }
        return extended;
    }

    private int connectionMask(IBlockAccess world, BlockPos pos, EnumFacing face, BlockPos.MutableBlockPos cursor) {
        EnumFacing up;
        EnumFacing left;
        switch (face) {
            case DOWN -> { up = EnumFacing.SOUTH; left = EnumFacing.WEST; }
            case UP -> { up = EnumFacing.NORTH; left = EnumFacing.WEST; }
            case NORTH -> { up = EnumFacing.UP; left = EnumFacing.EAST; }
            case SOUTH -> { up = EnumFacing.UP; left = EnumFacing.WEST; }
            case WEST -> { up = EnumFacing.UP; left = EnumFacing.NORTH; }
            case EAST -> { up = EnumFacing.UP; left = EnumFacing.SOUTH; }
            default -> throw new IllegalArgumentException("Unsupported face " + face);
        }
        EnumFacing down = up.getOpposite();
        EnumFacing right = left.getOpposite();
        int mask = 0;
        if (isConnected(world, pos, cursor, up.getXOffset() + left.getXOffset(), up.getYOffset() + left.getYOffset(), up.getZOffset() + left.getZOffset())) mask |= 1;
        if (isConnected(world, pos, cursor, up.getXOffset(), up.getYOffset(), up.getZOffset())) mask |= 1 << 1;
        if (isConnected(world, pos, cursor, up.getXOffset() + right.getXOffset(), up.getYOffset() + right.getYOffset(), up.getZOffset() + right.getZOffset())) mask |= 1 << 2;
        if (isConnected(world, pos, cursor, left.getXOffset(), left.getYOffset(), left.getZOffset())) mask |= 1 << 3;
        if (isConnected(world, pos, cursor, right.getXOffset(), right.getYOffset(), right.getZOffset())) mask |= 1 << 4;
        if (isConnected(world, pos, cursor, down.getXOffset() + left.getXOffset(), down.getYOffset() + left.getYOffset(), down.getZOffset() + left.getZOffset())) mask |= 1 << 5;
        if (isConnected(world, pos, cursor, down.getXOffset(), down.getYOffset(), down.getZOffset())) mask |= 1 << 6;
        if (isConnected(world, pos, cursor, down.getXOffset() + right.getXOffset(), down.getYOffset() + right.getYOffset(), down.getZOffset() + right.getZOffset())) mask |= 1 << 7;
        return mask;
    }

    private boolean isConnected(IBlockAccess world, BlockPos origin, BlockPos.MutableBlockPos cursor,
                                int offsetX, int offsetY, int offsetZ) {
        cursor.setPos(origin.getX() + offsetX, origin.getY() + offsetY, origin.getZ() + offsetZ);
        return world.getBlockState(cursor).getBlock() == this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerSprite(TextureMap map) {
        String namespace = getRegistryName().getNamespace();
        sideBase = map.registerSprite(new net.minecraft.util.ResourceLocation(namespace, "blocks/pile_block"));
        topBase = map.registerSprite(new net.minecraft.util.ResourceLocation(namespace, "blocks/pile_block_top"));
        sideConnected = map.registerSprite(new net.minecraft.util.ResourceLocation(namespace, "blocks/pile_block_ct"));
        topConnected = map.registerSprite(new net.minecraft.util.ResourceLocation(namespace, "blocks/pile_block_top_ct"));
        inputConnected = map.registerSprite(new net.minecraft.util.ResourceLocation(namespace, "blocks/pile_block_input_ct"));
        outputConnected = map.registerSprite(new net.minecraft.util.ResourceLocation(namespace, "blocks/pile_block_output_ct"));
        controlConnected = map.registerSprite(new net.minecraft.util.ResourceLocation(namespace, "blocks/pile_block_control_top_ct"));
        coreConnected = map.registerSprite(new net.minecraft.util.ResourceLocation(namespace, "blocks/pile_block_core_ct"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void bakeModel(ModelBakeEvent event) {
        for (int meta = 0; meta < META_COUNT; meta++) {
            TextureAtlasSprite[] bases = new TextureAtlasSprite[6];
            TextureAtlasSprite[] connected = new TextureAtlasSprite[6];
            for (EnumFacing face : EnumFacing.VALUES) {
                boolean vertical = face == EnumFacing.UP || face == EnumFacing.DOWN;
                bases[face.getIndex()] = vertical ? topBase : sideBase;
                connected[face.getIndex()] = connectedSprite(meta, vertical);
            }
            IBakedModel model = new BlockPileConnectedModel(bases, connected);
            event.getModelRegistry().putObject(
                    new ModelResourceLocation(getRegistryName(), "meta=" + meta), model);
        }
    }

    @SideOnly(Side.CLIENT)
    private TextureAtlasSprite connectedSprite(int meta, boolean vertical) {
        if (vertical) return meta == META_CONTROL ? controlConnected : topConnected;
        if (meta == META_FUEL_IN || meta == META_AIR_IN) return inputConnected;
        if (meta == META_FUEL_OUT || meta == META_AIR_OUT) return outputConnected;
        if (meta == META_CORE) return coreConnected;
        return sideConnected;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return state.getValue(META) == META_CORE ? new TileEntityPileCore() : new TileEntityPileBaseMK2();
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return false;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
                                  EntityPlayer player) {
        return new ItemStack(ModBlocks.pile_brick);
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
                                   boolean willHarvest) {
        if (!world.isRemote && !RESTORING.get()) {
            TileEntity tile = world.getTileEntity(pos);
            TileEntityPileCore core = tile instanceof TileEntityPileCore ? (TileEntityPileCore) tile :
                    tile instanceof TileEntityPileBaseMK2 ? ((TileEntityPileBaseMK2) tile).getCore() : null;
            if (core != null && !core.isInvalid()) {
                core.disassemble(pos, true);
            } else {
                restoreSinglePart(world, pos);
            }
            return false;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && !RESTORING.get()) {
            TileEntity tile = world.getTileEntity(pos);
            TileEntityPileCore core = tile instanceof TileEntityPileCore ? (TileEntityPileCore) tile :
                    tile instanceof TileEntityPileBaseMK2 ? ((TileEntityPileBaseMK2) tile).getCore() : null;
            if (core != null && !core.isInvalid()) core.disassemble(pos, true);
        }
        super.breakBlock(world, pos, state);
    }

    public static void restoreSinglePart(World world, BlockPos pos) {
        if (world == null || world.isRemote) return;
        boolean previous = RESTORING.get();
        RESTORING.set(true);
        try {
            world.setBlockState(pos, ModBlocks.pile_brick.getDefaultState(), 3);
        } finally {
            RESTORING.set(previous);
        }
    }

    @Override
    public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side,
                           float hitX, float hitY, float hitZ, EnumHand hand, ToolType tool) {
        if (tool != ToolType.HAND_DRILL) return false;
        BlockPos pos = new BlockPos(x, y, z);
        int meta = getMetaFromState(world.getBlockState(pos));
        if (meta == META_CORE || world.getTileEntity(pos) instanceof TileEntityPileCore) {
            if (!world.isRemote) {
                player.sendMessage(new TextComponentString("Cannot intersect Chicago Pile core")
                        .setStyle(new Style().setColor(TextFormatting.RED)));
            }
            return true;
        }
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityPileBaseMK2) {
            TileEntityPileCore core = ((TileEntityPileBaseMK2) tile).getCore();
            if (core != null) return core.drillChannel(pos, side.getOpposite(), player);
        }
        player.sendMessage(new TextComponentString("No Chicago Pile core found")
                .setStyle(new Style().setColor(TextFormatting.RED)));
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void printHook(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
        int meta = getMetaFromState(world.getBlockState(pos));
        List<String> text = new ArrayList<>();
        if (meta == META_FUEL_IN) text.add("Fuel Loading Port");
        if (meta == META_FUEL_OUT) text.add("Fuel Ejection Port");
        if (meta == META_AIR_IN) text.add("Air Inlet");
        if (meta == META_AIR_OUT) text.add("Air Outlet");
        if (meta == META_CONTROL) text.add("Control Rod Channel");
        if (meta == META_CORE) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityPileCore) {
                TileEntityPileCore core = (TileEntityPileCore) tile;
                text.add("Max Temp: " + Math.round(core.highestHeat) + " / " + TileEntityPileCore.MAX_HEAT + "°C");
            }
        }
        if (!text.isEmpty()) {
            ILookOverlay.printGeneric(event, I18n.format(getTranslationKey() + ".name"),
                    0xffff00, 0x404000, text);
        }
    }
}
