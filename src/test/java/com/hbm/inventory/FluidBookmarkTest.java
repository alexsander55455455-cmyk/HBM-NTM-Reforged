package com.hbm.inventory;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIDMulti;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class FluidBookmarkTest {
    @BeforeAll static void bootstrap() throws Exception { DataConsumerRecipeTest.bootstrap(); }
    static ItemFluidIDMulti item() { return (ItemFluidIDMulti)ModItems.fluid_identifier_multi; }
    static ItemStack stack() { return new ItemStack(ModItems.fluid_identifier_multi); }
    static int[] ids(int count) { return Arrays.stream(Fluids.getInNiceOrder()).filter(f->!f.hasNoID()).limit(count).mapToInt(FluidType::getID).toArray(); }
    static int[] read(ItemStack s) throws Exception { return (int[])ItemFluidIDMulti.class.getMethod("getBookmarks",ItemStack.class).invoke(null,s); }
    static void store(ItemStack s,int...ids) throws Exception { ItemFluidIDMulti.class.getMethod("setBookmarks",ItemStack.class,int[].class).invoke(null,s,ids); }
    static void control(ItemStack s,int...ids) { NBTTagCompound n=new NBTTagCompound();n.setIntArray("bookmarks",ids);item().receiveControl(s,n); }
    static ItemStack selected() { ItemStack s=stack();ItemFluidIDMulti.setType(s,Fluids.WATER,true);ItemFluidIDMulti.setType(s,Fluids.LAVA,false);s.getTagCompound().setString("custom","retained");return s; }
    @Test void legacyUntaggedItemHasNoBookmarksAndIsNotMutated() throws Exception { ItemStack s=stack();assertArrayEquals(new int[0],read(s));assertFalse(s.hasTagCompound()); }
    @Test void controlStoresAllNineBookmarksInOrder() throws Exception { ItemStack s=stack();int[] expected=ids(9);control(s,expected);assertArrayEquals(expected,read(s));assertArrayEquals(expected,s.getTagCompound().getIntArray("bookmarks")); }
    @Test void bookmarksDoNotChangeEitherFluidMetadataOrOtherNbt() throws Exception { ItemStack s=selected();int meta=s.getMetadata();control(s,ids(9));assertEquals(Fluids.WATER,ItemFluidIDMulti.getType(s,true));assertEquals(Fluids.LAVA,ItemFluidIDMulti.getType(s,false));assertEquals(meta,s.getMetadata());assertEquals("retained",s.getTagCompound().getString("custom")); }
    @Test void loadedLegacyNbtIsNormalizedWithoutWritingToTheItem() throws Exception { ItemStack s=selected();s.getTagCompound().setIntArray("bookmarks",new int[]{ids(2)[0],-1,ids(2)[0],ids(2)[1],Integer.MAX_VALUE});NBTTagCompound before=s.serializeNBT();assertArrayEquals(ids(2),read(s));assertEquals(before,s.serializeNBT()); }
    @Test void serverRejectsUnknownAndHiddenFluidIdsAndDuplicates() throws Exception { ItemStack s=stack();int[] valid=ids(2);control(s,valid[0],-1,valid[0],Integer.MAX_VALUE,valid[1]);assertArrayEquals(valid,read(s)); }
    @Test void serverKeepsOnlyNineValidUniqueIds() throws Exception { ItemStack s=stack();control(s,ids(12));assertArrayEquals(ids(9),read(s)); }
    @Test void wrongNbtTypeDoesNotEraseExistingBookmarks() throws Exception { ItemStack s=stack();store(s,ids(3));NBTTagCompound n=new NBTTagCompound();n.setString("bookmarks","invalid");item().receiveControl(s,n);assertArrayEquals(ids(3),read(s)); }
    @Test void emptyArrayClearsBookmarksAndKeepsSelections() throws Exception { ItemStack s=selected();store(s,ids(9));control(s);assertArrayEquals(new int[0],read(s));assertEquals(Fluids.WATER,ItemFluidIDMulti.getType(s,true));assertEquals(Fluids.LAVA,ItemFluidIDMulti.getType(s,false)); }
    @Test void bookmarksSurviveActualItemNbtRoundtrip() throws Exception { ItemStack s=selected();store(s,ids(9));ItemStack copy=new ItemStack(s.serializeNBT());assertArrayEquals(ids(9),read(copy));assertEquals(s.serializeNBT(),copy.serializeNBT()); }
    @Test void primarySelectionUpdatePreservesBookmarks() throws Exception { ItemStack s=selected();store(s,ids(9));NBTTagCompound n=new NBTTagCompound();n.setInteger("primary",Fluids.STEAM.getID());item().receiveControl(s,n);assertArrayEquals(ids(9),read(s));assertEquals(Fluids.STEAM,ItemFluidIDMulti.getType(s,true));assertEquals(Fluids.STEAM.getID(),s.getMetadata()); }
    @Test void secondarySelectionUpdatePreservesBookmarks() throws Exception { ItemStack s=selected();store(s,ids(9));NBTTagCompound n=new NBTTagCompound();n.setInteger("secondary",Fluids.STEAM.getID());item().receiveControl(s,n);assertArrayEquals(ids(9),read(s));assertEquals(Fluids.WATER.getID(),s.getMetadata()); }
    @Test void mutatingSourceArrayCannotChangeSavedBookmarks() throws Exception { ItemStack s=stack();int[] values=ids(2);store(s,values);values[0]=-1;assertArrayEquals(ids(2),read(s)); }
    @Test void mutatingReadArrayCannotChangeSavedBookmarks() throws Exception { ItemStack s=stack();store(s,ids(2));int[] values=read(s);values[0]=-1;assertArrayEquals(ids(2),read(s)); }
    @Test void controlPayloadDoesNotAliasSavedBookmarks() throws Exception { ItemStack s=stack();NBTTagCompound n=new NBTTagCompound();n.setIntArray("bookmarks",ids(2));item().receiveControl(s,n);n.getIntArray("bookmarks")[0]=-1;assertArrayEquals(ids(2),read(s)); }
    @Test void containerItemCopyRetainsBookmarksAndSelections() throws Exception { ItemStack s=selected();store(s,ids(9));ItemStack copy=item().getContainerItem(s);assertNotSame(s,copy);assertEquals(s.serializeNBT(),copy.serializeNBT()); }
}
