package com.hbm.render.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GunFatmanRenderRegistrationTest {

	private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
	private static final Path SEDNA_RENDER = PROJECT_ROOT.resolve(
			"src/main/java/com/hbm/render/item/weapon/sedna/ItemRenderFatMan.java");
	private static final Path CLIENT_PROXY = PROJECT_ROOT.resolve(
			"src/main/java/com/hbm/main/ClientProxy.java");

	@Test
	void sednaFatmanRendererMustNotAutoRegisterGunFatman() throws IOException {
		String source = Files.readString(SEDNA_RENDER, StandardCharsets.UTF_8);
		assertFalse(
				source.contains("@AutoRegister(item = \"gun_fatman\")"),
				"legacy gun_fatman must not use sedna ItemRenderFatMan via @AutoRegister");
	}

	@Test
	void clientProxyMustRegisterLegacyFatmanRendererForGunFatman() throws IOException {
		String source = Files.readString(CLIENT_PROXY, StandardCharsets.UTF_8);
		assertTrue(
				source.contains("registerItemRenderer(ModItems.gun_fatman, new com.hbm.render.item.weapon.ItemRenderFatMan())"),
				"ClientProxy must bind legacy ItemRenderFatMan to gun_fatman");
	}

	@Test
	void builtRegistrarMustNotBindGunFatmanToSednaFatmanRenderer() throws IOException {
		Path jar = findReleaseJar();
		try (JarFile jarFile = new JarFile(jar.toFile())) {
			ZipEntry entry = jarFile.getEntry("com/hbm/generated/GeneratedHBMRegistrar.class");
			assertTrue(entry != null, "release jar must contain GeneratedHBMRegistrar");

			byte[] bytes;
			try (InputStream in = jarFile.getInputStream(entry)) {
				bytes = in.readAllBytes();
			}

			String constantPool = new String(bytes, StandardCharsets.ISO_8859_1);
			assertFalse(
					constantPool.contains("gun_fatman"),
					"GeneratedHBMRegistrar must not reference gun_fatman after removing sedna @AutoRegister");
			assertFalse(
					constantPool.contains("com/hbm/render/item/weapon/sedna/ItemRenderFatMan"),
					"GeneratedHBMRegistrar must not reference sedna ItemRenderFatMan");
		}
	}

	private static Path findReleaseJar() throws IOException {
		Path libs = PROJECT_ROOT.resolve("build/libs");
		try (var stream = Files.list(libs)) {
			return stream
					.filter(p -> p.getFileName().toString().matches("HBM-NTM-Reforged-.*-alpha\\.jar"))
					.filter(p -> !p.getFileName().toString().contains("-dev"))
					.filter(p -> !p.getFileName().toString().contains("-api"))
					.filter(p -> !p.getFileName().toString().contains("-sources"))
					.filter(p -> !p.getFileName().toString().contains("-downgraded"))
					.sorted((a, b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
					.findFirst()
					.orElseThrow(() -> new IOException("release jar not found in build/libs; run gradle build first"));
		}
	}
}