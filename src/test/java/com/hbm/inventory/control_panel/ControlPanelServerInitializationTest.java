package com.hbm.inventory.control_panel;

import com.hbm.inventory.control_panel.controls.ControlType;
import com.hbm.inventory.control_panel.modular.NTMControlPanelRegistry;
import com.hbm.inventory.control_panel.modular.StockNodeLoader;
import com.hbm.inventory.control_panel.nodes.NodeInput;
import com.hbm.inventory.control_panel.types.DataValue;
import com.hbm.inventory.control_panel.types.DataValueFloat;
import com.hbm.inventory.control_panel.types.DataValueString;
import com.hbm.render.loader.IModelCustom;
import com.hbm.testutil.ForgeTestBootstrap;
import com.hbm.tileentity.machine.TileEntityControlPanel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnJre(JRE.JAVA_8)
class ControlPanelServerInitializationTest {
    @BeforeAll
    static void bootstrap() {
        ForgeTestBootstrap.ensureServerSide();
        if (!Bootstrap.isRegistered()) Bootstrap.register();
        ControlEvent.init();
        TileEntity.register("hbm:panel_initialize_test", TileEntityControlPanel.class);
        ControlRegistry.registry.put("initialize_test", new TestControl(null));
        NTMControlPanelRegistry.nbtNodeLoaders.add(new StockNodeLoader());
    }

    @Test
    void serverLoadCanSaveReloadAndPrepareChunkDataWithEventData() throws Exception {
        TileEntityControlPanel original = panelWithEventData();
        original.onLoad();
        assertInitialized(original);
        NBTTagCompound chunkData = original.getUpdateTag();

        TileEntityControlPanel restored = panel();
        restored.readFromNBT(chunkData);
        restored.onLoad();
        assertInitialized(restored);
        assertEquals(chunkData, restored.getUpdateTag());
    }

    @Test
    void serverReinitializationAfterEditingKeepsEventDataSerializable() throws Exception {
        TileEntityControlPanel tile = panelWithEventData();
        Method initialize = TileEntityControlPanel.class.getDeclaredMethod("reinitializePanelState");
        initialize.setAccessible(true);
        initialize.invoke(tile);
        assertInitialized(tile);
        assertDoesNotThrow(tile::getUpdateTag);

        eventData(tile).outputs.get(0).setDefault(new DataValueString("previous"));
        initialize.invoke(tile);
        assertInitialized(tile);
        assertDoesNotThrow(tile::getUpdateTag);
    }

    private static void assertInitialized(TileEntityControlPanel tile) {
        NodeInput input = eventData(tile);
        assertEquals("Event Data", input.name);
        assertEquals(2, input.outputs.size());
        assertNotNull(input.outputs.get(0).defaultValue);
        assertEquals(DataValue.DataType.STRING, input.outputs.get(0).type);
        assertEquals("", input.evaluate(0).toString());
        assertEquals(DataValue.DataType.NUMBER, input.outputs.get(1).type);
    }

    private static NodeInput eventData(TileEntityControlPanel tile) {
        return (NodeInput) tile.panel.controls.get(0).receiveNodeMap.get("initialize").nodes.get(0);
    }

    private static TileEntityControlPanel panelWithEventData() throws Exception {
        TileEntityControlPanel tile = panel();
        Control control = new TestControl(tile.panel);
        Map<String, DataValue> vars = new LinkedHashMap<>();
        vars.put("tag", new DataValueString("previous"));
        vars.put("from index", new DataValueFloat(0));
        NodeSystem system = new NodeSystem(control);
        system.nodes.add(new NodeInput(0, 0, "Event Data").setVars(vars));
        control.receiveNodeMap.put("initialize", system);
        tile.panel.controls.add(control);
        return tile;
    }

    private static TileEntityControlPanel panel() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        World world = (World) ((Unsafe) unsafeField.get(null)).allocateInstance(TestWorld.class);
        TileEntityControlPanel tile = new TileEntityControlPanel();
        tile.setWorld(world);
        tile.setPos(BlockPos.ORIGIN);
        return tile;
    }

    private static class TestControl extends Control {
        TestControl(ControlPanel panel) { super("Initialize test", "initialize_test", panel); }
        @Override public ControlType getControlType() { return null; }
        @Override public float[] getSize() { return new float[]{1, 1, 1}; }
        @Override public IModelCustom getModel() { return null; }
        @Override public ResourceLocation getGuiTexture() { return null; }
        @Override public Control newControl(ControlPanel panel) { return new TestControl(panel); }
        @Override public void populateDefaultNodes(List<ControlEvent> events) { }
    }

    private static class TestWorld extends World {
        private TestWorld() { super(null, null, null, null, false); }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return false; }
        @Override public IBlockState getBlockState(BlockPos pos) { return Blocks.AIR.getDefaultState(); }
        @Override public int getRedstonePower(BlockPos pos, EnumFacing facing) { return 0; }
    }
}
