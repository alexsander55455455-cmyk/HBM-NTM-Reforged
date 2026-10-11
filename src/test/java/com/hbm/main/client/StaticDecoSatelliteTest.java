package com.hbm.main.client;

import com.hbm.render.model.BakedModelMatrixUtil;
import org.junit.jupiter.api.Test;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticDecoSatelliteTest {

    @Test
    void satelliteBodiesRestOnTheFloorAndHeadsStayAboveTheirBaseInEveryOrientation() throws Exception {
        Path models = Paths.get(System.getProperty("user.dir"), "src/main/resources/assets/hbm/models");
        for (double yaw : new double[] {0, 90, 180, 270}) {
            Matrix4f transform = StaticDecoBakedModels.satelliteTransform(yaw);
            float[] base = heightBounds(models.resolve("sat_base.obj"), transform);
            assertEquals(0F, base[0], 0.0001F, "base must rest on the floor");
            assertEquals(5.25F, base[1], 0.0001F);
            for (String name : new String[] {"radar", "scanner", "mapper", "laser", "resonator"}) {
                float[] head = heightBounds(models.resolve("sat_" + name + ".obj"), transform);
                assertTrue(head[0] >= base[1] - 0.0001F, name + " head must stay above the base");
                assertTrue(head[1] > base[1], name + " head must be visible");
            }
            float[] relay = heightBounds(models.resolve("sat_foeq.obj"), transform);
            assertEquals(0F, relay[0], 0.0001F, "relay must rest on the floor");
            assertEquals(13.65F, relay[1], 0.0001F);
            Vector3f up = BakedModelMatrixUtil.transformNormal(transform, 0F, 1F, 0F);
            assertEquals(1F, up.y, 0.0001F, "satellites must stay upright");
        }
    }

    private static float[] heightBounds(Path model, Matrix4f transform) throws Exception {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (String line : Files.readAllLines(model, StandardCharsets.UTF_8)) {
            if (!line.startsWith("v ")) continue;
            String[] vertex = line.split("\\s+");
            Vector3f position = BakedModelMatrixUtil.transformPosition(transform,
                    Float.parseFloat(vertex[1]), Float.parseFloat(vertex[2]), Float.parseFloat(vertex[3]));
            min = Math.min(min, position.y);
            max = Math.max(max, position.y);
        }
        assertTrue(Float.isFinite(min), model.toString());
        return new float[] {min, max};
    }
}
