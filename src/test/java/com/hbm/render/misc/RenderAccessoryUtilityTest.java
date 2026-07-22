package com.hbm.render.misc;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RenderAccessoryUtilityTest {

	@Test
	void loadCapeIgnoresPlayersWithoutNetworkInfo() {
		assertDoesNotThrow(() -> RenderAccessoryUtility.loadCape(
				null,
				new ResourceLocation("hbm", "textures/models/capes/CapeTest.png")));
	}
}
