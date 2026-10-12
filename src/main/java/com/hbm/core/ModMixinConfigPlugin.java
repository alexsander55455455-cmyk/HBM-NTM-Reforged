package com.hbm.core;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

import static com.hbm.core.ModPresence.*;
import static org.objectweb.asm.Opcodes.*;

public class ModMixinConfigPlugin implements IMixinConfigPlugin {

    private static final String PACKAGE_PREFIX = "com.hbm.mixin.mod.";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(PACKAGE_PREFIX)) {
            return true;
        }

        String suffix = mixinClassName.substring(PACKAGE_PREFIX.length());
        int separator = suffix.indexOf('.');
        if (separator < 0) {
            return true;
        }

        String group = suffix.substring(0, separator);
        return switch (group) {
            case "neonium" -> NEONIUM;
            case "nothirium" -> NOTHIRIUM;
            case "optifine" -> OPTIFINE;
            case "celeritas" -> CELERITAS;
            case "jei" -> JEI;
            default -> true;
        };
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!mixinClassName.equals(PACKAGE_PREFIX + "fallout.MixinCursedAddonFalloutRainGuard")) return;

        // The addon's merged completion hook otherwise registers fallout even when doFallout is false.
        for (MethodNode method : targetClass.methods) {
            if (!method.name.endsWith("leafia$onSetDead") || (method.access & ACC_STATIC) != 0
                    || !method.desc.equals("(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V")) continue;
            LabelNode radioactive = new LabelNode();
            InsnList guard = new InsnList();
            guard.add(new VarInsnNode(ALOAD, 0));
            guard.add(new FieldInsnNode(GETFIELD, targetClass.name, "doFallout", "Z"));
            guard.add(new JumpInsnNode(IFNE, radioactive));
            guard.add(new InsnNode(RETURN));
            guard.add(radioactive);
            method.instructions.insert(guard);
        }
    }
}
