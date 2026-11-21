package org.polyfrost.polydium.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.polyfrost.polydium.PolydiumConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class PolydiumMixinPlugin implements IMixinConfigPlugin {
    private static final String PACKAGE = "org.polyfrost.polydium.mixin.";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        PolydiumConfig config = PolydiumConfig.INSTANCE;
        String feature = mixinClassName.substring(PACKAGE.length(), mixinClassName.indexOf('.', PACKAGE.length()));
        return switch (feature) {
            case "particle" -> config.particleCollision;
            case "entity" -> config.entityBlockCaching;
            case "world" -> config.particleCollision || config.entityBlockCaching;
            case "hud" -> config.hudCaching;
            case "srv" -> config.connectSrvLookup;
            case "limiter" -> config.preciseFrameLimiter;
            case "border" -> config.worldBorderCaching;
            default -> true;
        };
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
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
    }
}
