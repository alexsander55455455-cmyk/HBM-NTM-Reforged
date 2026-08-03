package com.hbm.render.model;

import com.hbm.blocks.machine.pile.BlockPile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1.12 baked-model equivalent of the original four-fragment Chicago Pile CT renderer.
 */
@SideOnly(Side.CLIENT)
public class BlockPileConnectedModel extends AbstractBakedModel {

    private static final int FULL = 0;
    private static final int CONNECTED = 4;
    private static final int JUNCTION = 8;
    private static final int HORIZONTAL = 12;
    private static final int VERTICAL = 16;

    private final TextureAtlasSprite[] baseSprites;
    private final TextureAtlasSprite[] connectedSprites;
    @SuppressWarnings("unchecked")
    private final List<BakedQuad>[][] faceCache = new List[6][256];
    private final List<BakedQuad> inventoryQuads;

    public BlockPileConnectedModel(TextureAtlasSprite[] baseSprites, TextureAtlasSprite[] connectedSprites) {
        super(BakedModelTransforms.standardBlock());
        this.baseSprites = baseSprites.clone();
        this.connectedSprites = connectedSprites.clone();
        this.inventoryQuads = bakeInventory();
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        if (state == null) return side == null ? inventoryQuads : Collections.emptyList();
        if (side == null) return Collections.emptyList();

        int mask = 0;
        if (state instanceof IExtendedBlockState extended) {
            Integer value = extended.getValue(BlockPile.FACE_MASKS[side.getIndex()]);
            if (value != null) mask = value & 0xFF;
        }

        List<BakedQuad> cached = faceCache[side.getIndex()][mask];
        if (cached != null) return cached;

        int topLeft = cornerIndex(mask, 3, 0, 1, 0);
        int topRight = cornerIndex(mask, 4, 2, 1, 1);
        int bottomLeft = cornerIndex(mask, 3, 5, 6, 2);
        int bottomRight = cornerIndex(mask, 4, 7, 6, 3);

        List<BakedQuad> quads = new ArrayList<>(4);
        quads.add(bakeFragment(side, 0, topLeft));
        quads.add(bakeFragment(side, 1, topRight));
        quads.add(bakeFragment(side, 2, bottomLeft));
        quads.add(bakeFragment(side, 3, bottomRight));
        cached = Collections.unmodifiableList(quads);
        faceCache[side.getIndex()][mask] = cached;
        return cached;
    }

    private static int cornerIndex(int mask, int horizontalBit, int cornerBit, int verticalBit, int quadrant) {
        boolean horizontal = (mask & (1 << horizontalBit)) != 0;
        boolean corner = (mask & (1 << cornerBit)) != 0;
        boolean vertical = (mask & (1 << verticalBit)) != 0;

        int type;
        if (vertical && horizontal && corner) type = CONNECTED;
        else if (vertical && horizontal) type = JUNCTION;
        else if (vertical) type = VERTICAL;
        else if (horizontal) type = HORIZONTAL;
        else type = FULL;
        return type | quadrant;
    }

    private BakedQuad bakeFragment(EnumFacing face, int quadrant, int fragment) {
        TextureAtlasSprite sprite = fragment < CONNECTED
                ? baseSprites[face.getIndex()]
                : connectedSprites[face.getIndex()];

        int column = (quadrant & 1);
        int row = (quadrant & 2) >> 1;
        float cellSize;
        if (fragment < CONNECTED) {
            cellSize = 8.0F;
        } else {
            if (fragment >= VERTICAL || fragment >= JUNCTION && fragment < HORIZONTAL) column += 2;
            if (fragment >= HORIZONTAL && fragment < VERTICAL || fragment >= JUNCTION && fragment < HORIZONTAL) row += 2;
            cellSize = 4.0F;
        }

        float u0 = column * cellSize;
        float v0 = row * cellSize;
        return bakeFace(face, quadrant, sprite, u0, v0, u0 + cellSize, v0 + cellSize);
    }

    private static BakedQuad bakeFace(EnumFacing face, int quadrant, TextureAtlasSprite sprite,
                                      float u0, float v0, float u1, float v1) {
        boolean left = (quadrant & 1) == 0;
        boolean top = (quadrant & 2) == 0;
        float lo = 0.0F;
        float mid = 8.0F;
        float hi = 16.0F;
        Vector3f from;
        Vector3f to;

        switch (face) {
            case DOWN -> {
                float minX = left ? lo : mid;
                float minZ = top ? mid : lo;
                from = new Vector3f(minX, lo, minZ);
                to = new Vector3f(minX + mid, hi, minZ + mid);
            }
            case UP -> {
                float minX = left ? lo : mid;
                float minZ = top ? lo : mid;
                from = new Vector3f(minX, lo, minZ);
                to = new Vector3f(minX + mid, hi, minZ + mid);
            }
            case NORTH -> {
                float minX = left ? mid : lo;
                float minY = top ? mid : lo;
                from = new Vector3f(minX, minY, lo);
                to = new Vector3f(minX + mid, minY + mid, hi);
            }
            case SOUTH -> {
                float minX = left ? lo : mid;
                float minY = top ? mid : lo;
                from = new Vector3f(minX, minY, lo);
                to = new Vector3f(minX + mid, minY + mid, hi);
            }
            case WEST -> {
                float minY = top ? mid : lo;
                float minZ = left ? lo : mid;
                from = new Vector3f(lo, minY, minZ);
                to = new Vector3f(hi, minY + mid, minZ + mid);
            }
            case EAST -> {
                float minY = top ? mid : lo;
                float minZ = left ? mid : lo;
                from = new Vector3f(lo, minY, minZ);
                to = new Vector3f(hi, minY + mid, minZ + mid);
            }
            default -> throw new IllegalArgumentException("Unsupported face " + face);
        }

        BlockFaceUV uv = new BlockFaceUV(new float[]{u0, v0, u1, v1}, 0);
        BlockPartFace partFace = new BlockPartFace(face, -1, "", uv);
        return new FaceBakery().makeBakedQuad(from, to, partFace, sprite, face,
                TRSRTransformation.identity(), null, true, true);
    }

    private List<BakedQuad> bakeInventory() {
        List<BakedQuad> quads = new ArrayList<>(6);
        Vector3f from = new Vector3f(0.0F, 0.0F, 0.0F);
        Vector3f to = new Vector3f(16.0F, 16.0F, 16.0F);
        for (EnumFacing face : EnumFacing.VALUES) {
            BlockFaceUV uv = new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0);
            BlockPartFace partFace = new BlockPartFace(face, -1, "", uv);
            quads.add(new FaceBakery().makeBakedQuad(from, to, partFace, baseSprites[face.getIndex()], face,
                    TRSRTransformation.identity(), null, true, true));
        }
        return Collections.unmodifiableList(quads);
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return baseSprites[EnumFacing.NORTH.getIndex()];
    }
}
