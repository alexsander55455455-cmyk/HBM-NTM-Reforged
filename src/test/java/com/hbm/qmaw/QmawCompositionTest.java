package com.hbm.qmaw;

import com.google.gson.*;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class QmawCompositionTest {
    Map<String,QuickManualAndWiki> pages;
    Map<ComparableStack,QuickManualAndWiki> triggers;
    @BeforeAll static void bootstrap() throws Exception {
        Method m=Class.forName("com.hbm.inventory.DataConsumerRecipeTest").getDeclaredMethod("bootstrap");m.setAccessible(true);m.invoke(null);
    }
    @BeforeEach void remember() { pages=new HashMap<>(QMAWLoader.qmaw);triggers=new HashMap<>(QMAWLoader.triggers);QMAWLoader.qmaw.clear();QMAWLoader.triggers.clear(); }
    @AfterEach void restore() { QMAWLoader.qmaw.clear();QMAWLoader.qmaw.putAll(pages);QMAWLoader.triggers.clear();QMAWLoader.triggers.putAll(triggers); }
    static String compose(String text,String lang) throws Exception { Method m=GuiQMAW.class.getDeclaredMethod("compose",String.class,String.class);m.setAccessible(true);return (String)m.invoke(null,text,lang); }
    static void page(String name,String en,String ru) { QuickManualAndWiki p=new QuickManualAndWiki(name);if(en!=null)p.addLang("en_us",en);if(ru!=null)p.addLang("ru_ru",ru);QMAWLoader.qmaw.put(name,p); }
    static Item coal() throws Exception { return (Item)ModItems.class.getField("coal_eternal").get(null); }
    static JsonObject article(String path) throws Exception { InputStream input=QmawCompositionTest.class.getResourceAsStream("/assets/hbm/manual/"+path);assertNotNull(input,path);try(Reader reader=new InputStreamReader(input,StandardCharsets.UTF_8)){return new JsonParser().parse(reader).getAsJsonObject();} }
    @Test void plainTextAndMarkupArePreserved() throws Exception { assertEquals("§2Text<br>[[Link]]",compose("§2Text<br>[[Link]]","en_us")); }
    @Test void emptyTextRemainsEmpty() throws Exception { assertEquals("",compose("","en_us")); }
    @Test void templateExpandsInPlace() throws Exception { page("A","body",null);assertEquals("before body after",compose("before {{A}} after","en_us")); }
    @Test void currentLanguageIsPreferred() throws Exception { page("A","english","русский");assertEquals("русский",compose("{{A}}","ru_ru")); }
    @Test void missingTranslationFallsBackToOwnLowercaseEnglish() throws Exception { page("A","english",null);assertEquals("english",compose("{{A}}","ru_ru")); }
    @Test void missingPageFallsBackToItsName() throws Exception { assertEquals("Missing",compose("{{Missing}}","en_us")); }
    @Test void pageWithoutEitherLanguageFallsBackToName() throws Exception { page("A",null,null);assertEquals("A",compose("{{A}}","en_us")); }
    @Test void emptyLocalizedBodyIsNotMistakenForMissing() throws Exception { page("A","english","");assertEquals("xy",compose("x{{A}}y","ru_ru")); }
    @Test void repeatedTemplatesExpand() throws Exception { page("A","body",null);assertEquals("body/body",compose("{{A}}/{{A}}","en_us")); }
    @Test void nestedTemplatesExpandWithSameLanguage() throws Exception { page("A","{{B}}","{{B}}");page("B","en","ru");assertEquals("ru",compose("{{A}}","ru_ru")); }
    @Test void multipleDifferentTemplatesExpandInOrder() throws Exception { page("A","one",null);page("B","two",null);assertEquals("one two",compose("{{A}} {{B}}","en_us")); }
    @Test void unmatchedOpeningIsPreserved() throws Exception { assertEquals("before {{A",compose("before {{A","en_us")); }
    @Test void literalClosingBeforeAValidTemplateIsPreserved() throws Exception { page("A","body",null);assertEquals("}} body",compose("}} {{A}}","en_us")); }
    @Test void directRecursionStops() throws Exception { page("A","{{A}}",null);assertTimeoutPreemptively(Duration.ofSeconds(2),()->assertEquals("{{A}}",compose("{{A}}","en_us"))); }
    @Test void mutualRecursionStops() throws Exception { page("A","{{B}}",null);page("B","{{A}}",null);assertTimeoutPreemptively(Duration.ofSeconds(2),()->assertEquals("{{A}}",compose("{{A}}","en_us"))); }
    @Test void selfDuplicatingTemplateHasBoundedLinearExpansion() throws Exception { page("A","{{A}}{{A}}",null);assertTimeoutPreemptively(Duration.ofSeconds(2),()->assertEquals(505,compose("{{A}}","en_us").length())); }
    @Test void eternalCoalHasExactOriginalBurnTime() throws Exception { assertEquals(3200,TileEntityFurnace.getItemBurnTime(new ItemStack(coal()))); }
    @Test void eternalCoalIsUnstackableAndHiddenFromCreative() throws Exception { assertEquals(1,new ItemStack(coal()).getMaxStackSize());assertNull(coal().getCreativeTab()); }
    @Test void eternalCoalReturnsOneOfItself() throws Exception { ItemStack stack=new ItemStack(coal());assertTrue(coal().hasContainerItem(stack));ItemStack result=coal().getContainerItem(stack);assertSame(coal(),result.getItem());assertEquals(1,result.getCount()); }
    @Test void repeatedFuelContainerCyclesDoNotConsumeEternalCoal() throws Exception { ItemStack fuel=new ItemStack(coal());for(int i=0;i<32;i++){assertEquals(3200,TileEntityFurnace.getItemBurnTime(fuel));fuel=coal().getContainerItem(fuel);assertEquals(1,fuel.getCount());assertSame(coal(),fuel.getItem());} }
    @Test void eternalCoalSurvivesItemNbtRoundtrip() throws Exception { ItemStack original=new ItemStack(coal());ItemStack restored=new ItemStack(original.serializeNBT());assertSame(coal(),restored.getItem());assertEquals(3200,TileEntityFurnace.getItemBurnTime(restored));assertSame(coal(),coal().getContainerItem(restored).getItem()); }
    @Test void existingInfernalCoalKeepsItsOwnBurnTime() throws Exception { assertEquals(4800,TileEntityFurnace.getItemBurnTime(new ItemStack(ModItems.coal_infernal)));assertNotSame(ModItems.coal_infernal,coal()); }
    @Test void importedChlorineArticleUsesOwnMetadataAndLocales() throws Exception { JsonObject json=article("fluids/chlorine_gas.json");assertEquals(Fluids.CHLORINE.getID(),json.getAsJsonArray("icon").get(2).getAsInt());QMAWLoader.registerJson("chlorine_gas.json",json);QuickManualAndWiki page=QMAWLoader.qmaw.get("Chlorine Gas");assertNotNull(page.contents.get("en_us"));assertNotNull(page.contents.get("ru_ru"));assertSame(page,QMAWLoader.triggers.get(new ComparableStack(new ItemStack(ModItems.fluid_icon,1,Fluids.CHLORINE.getID())).makeSingular())); }
    @Test void importedOilTemplateComposesInBothLanguages() throws Exception { JsonObject json=article("fluids/template_oil1.json");QMAWLoader.registerJson("template_oil1.json",json);String name=json.get("name").getAsString();for(String lang:new String[]{"en_us","ru_ru"}){String body=QMAWLoader.qmaw.get(name).contents.get(lang);assertNotNull(body);assertEquals(body,compose("{{"+name+"}}",lang));} }
    @Test void importedEternalCoalArticleHasARealWorkingTrigger() throws Exception { QMAWLoader.registerJson("etcoal.json",article("material/etcoal.json"));assertSame(QMAWLoader.qmaw.get("Eternal Coal"),QMAWLoader.triggers.get(new ComparableStack(new ItemStack(coal())).makeSingular())); }
}
