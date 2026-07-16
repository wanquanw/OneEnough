package com.mafuyu404.oneenoughitem.mixin;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mafuyu404.oneenoughitem.init.ItemReplacementCache;
import com.mafuyu404.oneenoughitem.init.OEIReplacementStrategy;
import com.mafuyu404.oneenoughitem.init.config.OEIConfig;
import com.mafuyu404.oneenoughitem.util.JsonReloadMixinHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class SimpleJsonResourceReloadListenerMixin {

    /**
     * In 26.1.2, {@code prepare} returns codec-decoded values (for example,
     * {@code Advancement}), rather than JSON.  Modify the JSON map before the
     * listener's codec reads it instead of casting the decoded result.
     */
    @Redirect(
            method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/SimpleJsonResourceReloadListener;scanDirectory(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/FileToIdConverter;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V"
            )
    )
    private static <T> void oei$rewriteJsonBeforeDecoding(ResourceManager resourceManager,
                                                            FileToIdConverter lister,
                                                            DynamicOps<JsonElement> ops,
                                                            Codec<T> codec,
                                                            Map<Identifier, T> results) {
        String directory = normalizeDirectory(lister.prefix());
        SimpleJsonResourceReloadListener.scanDirectoryWithModifier(
                resourceManager, lister, ops, codec, results,
                jsonResults -> JsonReloadMixinHelper.processJsonReload(
                        directory,
                        resourceManager,
                        jsonResults,
                        new OEIReplacementStrategy(),
                        "recipe".equals(directory) ? ItemReplacementCache::beginReloadOverride : null,
                        ItemReplacementCache::hasAnyMappings,
                        modId -> OEIConfig.get().defaultRules().toRules(),
                        "oei"
                )
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
