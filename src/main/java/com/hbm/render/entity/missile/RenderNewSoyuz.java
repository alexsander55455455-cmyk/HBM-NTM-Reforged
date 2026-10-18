package com.hbm.render.entity.missile;
import com.hbm.entity.missile.EntityRocketSoyuz;
import com.hbm.interfaces.AutoRegister;
import com.hbm.items.weapon.sedna.factory.LegoClient;
import com.hbm.main.ResourceManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
@AutoRegister(factory = "FACTORY")
public class RenderNewSoyuz extends Render<EntityRocketSoyuz> {
    public static final IRenderFactory<EntityRocketSoyuz> FACTORY = RenderNewSoyuz::new;
    public RenderNewSoyuz(RenderManager manager) { super(manager); }
    @Override public void doRender(EntityRocketSoyuz rocket, double x, double y, double z, float yaw, float interp) {
        int tex=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),shade=GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        FloatBuffer color=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_CURRENT_COLOR,color);
        boolean lighting=GL11.glIsEnabled(GL11.GL_LIGHTING),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),blend=GL11.glIsEnabled(GL11.GL_BLEND),alpha=GL11.glIsEnabled(GL11.GL_ALPHA_TEST),depth=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),light0=GL11.glIsEnabled(GL11.GL_LIGHT0),light1=GL11.glIsEnabled(GL11.GL_LIGHT1),material=GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
        int source=GL11.glGetInteger(0x80C9),dest=GL11.glGetInteger(0x80C8),sourceAlpha=GL11.glGetInteger(0x80CB),destAlpha=GL11.glGetInteger(0x80CA),function=GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC),face=GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_FACE),parameter=GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_PARAMETER);float ref=GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
        try {
            GL11.glTranslated(x,y,z);GlStateManager.enableLighting();GlStateManager.enableCull();GlStateManager.shadeModel(GL11.GL_SMOOTH);
            com.hbm.render.misc.SoyuzPronter.prontSoyuz(rocket.getSkin());
            Vec3d toward=new Vec3d(x,y,z).normalize().scale(-1.5);GL11.glTranslated(toward.x,toward.y,toward.z);
            for(double[] offset : new double[][] {{0,0},{2.5,0},{-2.5,0},{0,2.5},{0,-2.5}}) {
                GL11.glPushMatrix();
                GL11.glTranslated(offset[0],0,offset[1]);GL11.glScaled(1.5,1.5,1.5);
                LegoClient.renderFlare(rocket,interp,1F,.75F,.5F);
                GL11.glPopMatrix();
            }
        } finally {
            GlStateManager.color(color.get(0),color.get(1),color.get(2),color.get(3));
            GlStateManager.bindTexture(tex);GlStateManager.shadeModel(shade);GlStateManager.depthMask(depth);GlStateManager.alphaFunc(function,ref);GlStateManager.tryBlendFuncSeparate(source,dest,sourceAlpha,destAlpha);GlStateManager.colorMaterial(face,parameter);
            if(lighting)GlStateManager.enableLighting();else GlStateManager.disableLighting();if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();if(alpha)GlStateManager.enableAlpha();else GlStateManager.disableAlpha();if(light0)GlStateManager.enableLight(0);else GlStateManager.disableLight(0);if(light1)GlStateManager.enableLight(1);else GlStateManager.disableLight(1);if(material)GlStateManager.enableColorMaterial();else GlStateManager.disableColorMaterial();
            GL11.glPopMatrix();GL11.glPopAttrib();
        }
    }
    @Override protected ResourceLocation getEntityTexture(EntityRocketSoyuz rocket) { return ResourceManager.soyuz_payload; }
}
