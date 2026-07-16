package cc.sighs.oneenoughfluid.mixin;

import cc.sighs.oneenoughfluid.init.FluidReplacementCache;
import cc.sighs.oneenoughfluid.init.OEFConfig;
import cc.sighs.oneenoughfluid.init.OEFReplacementStrategy;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import cc.sighs.oneenoughitem.util.JsonReloadMixinHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;
import java.util.function.Consumer;

@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class SimpleJsonResourceReloadListenerMixin {

    /**
     * OEI invokes {@code scanDirectoryWithModifier} while preparing JSON reload
     * listeners.  Add fluid replacements to that same raw JSON map before OEI's
     * consumer and the codec decode it, so both replacement domains compose.
     */
    @Inject(
            method = "scanDirectoryWithModifier",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static <T> void oef$rewriteJsonBeforeDecoding(ResourceManager resourceManager,
                                                           FileToIdConverter lister,
                                                           DynamicOps<JsonElement> ops,
                                                           Codec<T> codec,
                                                           Map<Identifier, T> results,
                                                           Consumer<Map<Identifier, JsonElement>> jsonConsumer,
                                                           CallbackInfo ci,
                                                           Map<Identifier, JsonElement> jsonResults) {
        String directory = normalizeDirectory(lister.prefix());
        JsonReloadMixinHelper.processJsonReload(
                directory, resourceManager, jsonResults, new OEFReplacementStrategy(),
                "recipe".equals(directory) ? FluidReplacementCache::beginReloadOverride : null,
                FluidReplacementCache::hasAnyMappings,
                modId -> { var cfg = OEFConfig.get(); return cfg != null ? cfg.defaultRules().toRules() : null; },
                "oef"
        );
    }

    private static String normalizeDirectory(String prefix) {
        return switch (prefix) {
            case "recipes" -> "recipe";
            case "advancements" -> "advancement";
            case "loot_tables" -> "loot_table";
            default -> prefix;
        };
    }
}
