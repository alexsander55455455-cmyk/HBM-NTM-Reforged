package com.hbm.core;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.objectweb.asm.Opcodes.*;

class CursedAddonFalloutGuardTest {

    private static final String GUARD_MIXIN = "com.hbm.mixin.mod.fallout.MixinCursedAddonFalloutRainGuard";
    private static final String HOOK = "handler$abc123$leafia$onSetDead";
    private static final String HOOK_DESC = "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";

    @Test
    void nonradioactiveExplosionDoesNotRunAddonFalloutHook() throws Exception {
        assertEquals(0, invokeHook(false, HOOK, GUARD_MIXIN));
    }

    @Test
    void nuclearExplosionStillRunsAddonFalloutHook() throws Exception {
        assertEquals(1, invokeHook(true, HOOK, GUARD_MIXIN));
    }

    @Test
    void unrelatedAddonHooksAreUnchanged() throws Exception {
        assertEquals(1, invokeHook(false, "handler$abc123$unrelated", GUARD_MIXIN));
    }

    @Test
    void otherMixinPostApplyDoesNotPatchFalloutHook() throws Exception {
        assertEquals(1, invokeHook(false, HOOK, "com.hbm.mixin.mod.unrelated.MixinOther"));
    }

    @Test
    void absentAddonNeedsNoPatch() {
        ClassNode target = fixture("unrelated");
        MethodNode method = target.methods.get(1);
        int instructions = method.instructions.size();
        new ModMixinConfigPlugin().postApply("com.hbm.entity.effect.EntityFalloutRain", target, GUARD_MIXIN, null);
        assertEquals(instructions, method.instructions.size());
    }

    private static int invokeHook(boolean fallout, String methodName, String mixinName) throws Exception {
        ClassNode target = fixture(methodName);
        new ModMixinConfigPlugin().postApply("com.hbm.entity.effect.EntityFalloutRain", target, mixinName, null);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        target.accept(writer);
        byte[] bytes = writer.toByteArray();
        Class<?> type = new ClassLoader(CursedAddonFalloutGuardTest.class.getClassLoader()) {
            Class<?> define() { return defineClass(null, bytes, 0, bytes.length); }
        }.define();
        Object instance = type.getConstructor().newInstance();
        type.getField("doFallout").setBoolean(instance, fallout);
        type.getMethod(methodName, CallbackInfo.class).invoke(instance, new Object[] {null});
        return type.getField("falloutAreas").getInt(instance);
    }

    private static ClassNode fixture(String methodName) {
        ClassNode target = new ClassNode();
        target.version = V1_8;
        target.access = ACC_PUBLIC;
        target.name = "test/FalloutRainHook";
        target.superName = "java/lang/Object";
        target.fields.add(new FieldNode(ACC_PUBLIC, "doFallout", "Z", null, null));
        target.fields.add(new FieldNode(ACC_PUBLIC, "falloutAreas", "I", null, null));
        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        target.methods.add(constructor);
        // Model the merged addon's observable side effect, counting newly registered fallout areas.
        MethodNode hook = new MethodNode(ACC_PUBLIC, methodName, HOOK_DESC, null, null);
        hook.visitVarInsn(ALOAD, 0);
        hook.visitInsn(DUP);
        hook.visitFieldInsn(GETFIELD, target.name, "falloutAreas", "I");
        hook.visitInsn(ICONST_1);
        hook.visitInsn(IADD);
        hook.visitFieldInsn(PUTFIELD, target.name, "falloutAreas", "I");
        hook.visitInsn(RETURN);
        target.methods.add(hook);
        return target;
    }
}
