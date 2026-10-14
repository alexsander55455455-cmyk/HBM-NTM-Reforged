package com.hbm.render.weapon;

import com.hbm.render.item.weapon.sedna.ItemRenderMK108;
import com.hbm.testutil.ForgeTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import java.net.URL;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class Mk108BeltGeometryTest {
    @BeforeAll static void bootstrap() {
        ForgeTestBootstrap.ensureServerSide();
        if (Launch.classLoader == null) Launch.classLoader = new LaunchClassLoader(new URL[0]);
    }
    private static double[][] positions(double reload) throws Exception {
        Method method = ItemRenderMK108.class.getDeclaredMethod("getShellPositions", double.class);
        method.setAccessible(true);
        return (double[][]) method.invoke(null, reload);
    }

    @Test
    void loadedBeltReachesReceiverInsteadOfCurvingOutward() throws Exception {
        double[][] belt = positions(1);
        double[] last = belt[belt.length - 1];
        // The authored Gun mesh spans x=-1.375..1.375; the old endpoint was outside it.
        assertTrue(last[0] >= -1.375 && last[0] <= 1.375, "Belt must reach the receiver mesh");
        assertTrue(last[0] < belt[0][0], "Rounds feed from the drum toward the receiver");
        assertTrue(last[1] > belt[0][1]);
    }

    @Test
    void reloadKeepsDrumConnectionAndContinuousLinks() throws Exception {
        for (double reload : new double[]{0, 0.25, 0.5, 0.75, 1}) {
            double[][] belt = positions(reload);
            assertEquals(1.375, belt[0][0], 0.00001);
            assertEquals(-2.875, belt[0][1], 0.00001);
            for (int i = 1; i < belt.length; i++) {
                assertEquals(0.53125, Math.hypot(belt[i][0] - belt[i - 1][0], belt[i][1] - belt[i - 1][1]), 0.00001);
            }
        }
    }
}
