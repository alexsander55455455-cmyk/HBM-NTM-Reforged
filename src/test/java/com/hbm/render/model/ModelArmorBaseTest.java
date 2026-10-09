package com.hbm.render.model;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelArmorBaseTest {

    @Test
    void followsTheModelSuppliedByTheActiveArmorLayer() {
        ModelArmorBase armor = new TestArmorModel();
        ModelBiped player = new ModelBiped();
        player.bipedHead.rotateAngleX = 0.4F;
        player.bipedBody.rotateAngleY = 0.2F;
        player.bipedLeftArm.rotateAngleZ = 0.3F;
        player.bipedRightArm.rotateAngleX = -1.2F;
        player.bipedLeftLeg.rotateAngleX = 0.8F;
        player.bipedRightLeg.rotateAngleX = -0.8F;
        armor.setModelAttributes(player);

        armor.setRotationAngles(0F, 0F, 0F, 0F, 0F, 0.0625F, null);

        assertEquals(0.4F, armor.head.rotateAngleX);
        assertEquals(0.2F, armor.body.rotateAngleY);
        assertEquals(0.3F, armor.leftArm.rotateAngleZ);
        assertEquals(-1.2F, armor.rightArm.rotateAngleX);
        assertEquals(0.8F, armor.leftLeg.rotateAngleX);
        assertEquals(-0.8F, armor.rightLeg.rotateAngleX);
        assertEquals(armor.leftLeg.rotateAngleX, armor.leftFoot.rotateAngleX);
        assertEquals(armor.rightLeg.rotateAngleX, armor.rightFoot.rotateAngleX);

        player.bipedRightArm.rotateAngleX = 0.7F;
        armor.setRotationAngles(0F, 0F, 0F, 0F, 0F, 0.0625F, null);
        assertEquals(0.7F, armor.rightArm.rotateAngleX);

        ModelBiped otherPlayer = new ModelBiped();
        otherPlayer.bipedRightArm.rotateAngleX = -0.5F;
        armor.setModelAttributes(otherPlayer);
        armor.setRotationAngles(0F, 0F, 0F, 0F, 0F, 0.0625F, null);
        assertEquals(-0.5F, armor.rightArm.rotateAngleX);
    }

    @Test
    void repeatedSneakingPassesDoNotAccumulateOffsets() {
        ModelArmorBase armor = new TestArmorModel();
        ModelBiped player = new ModelBiped();
        player.isSneak = true;
        player.bipedHead.rotationPointY = 1F;
        armor.setModelAttributes(player);

        armor.setRotationAngles(0F, 0F, 0F, 0F, 0F, 0.0625F, null);
        float headY = armor.head.rotationPointY;
        armor.setRotationAngles(0F, 0F, 0F, 0F, 0F, 0.0625F, null);
        assertEquals(headY, armor.head.rotationPointY);

        player.isSneak = false;
        armor.setRotationAngles(0F, 0F, 0F, 0F, 0F, 0.0625F, null);
        assertEquals(0F, armor.head.offsetY);
        assertEquals(0F, armor.body.offsetY);
        assertEquals(0F, armor.leftLeg.offsetZ);
        assertEquals(0F, armor.rightFoot.offsetZ);
        assertEquals(1F, armor.head.rotationPointY);
    }

    private static class TestArmorModel extends ModelArmorBase {
        TestArmorModel() {
            super(1);
        }

        @Override
        protected void renderArmor(Entity entity, float scale) {
        }
    }
}
