package cc.sighs.oneenoughitem.mixin;

import cc.sighs.oneenoughitem.Oneenoughitem;
import cc.sighs.oneenoughitem.client.ClientContext;
import cc.sighs.oneenoughitem.init.ItemReplacementCache;
import cc.sighs.oneenoughitem.init.config.OEIConfig;
import cc.sighs.oneenoughitem.util.ReplacementControl;
import cc.sighs.oneenoughitem.util.Utils;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(value = ItemStack.class)
public abstract class ItemStackMixin {
    @Mutable
    @Shadow
    @Final
    private Holder<Item> item;

    @Mutable
    @Shadow(remap = false)
    @Final
    PatchedDataComponentMap components;

    @Shadow
    public abstract Item getItem();

    @Inject(method = "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/PatchedDataComponentMap;)V", at = @At("TAIL"))
    private void replaceWithComponents(Holder<Item> itemHolder, int count, PatchedDataComponentMap components, CallbackInfo ci) {
        performReplacement();
    }

    private void performReplacement() {
        if (this.item == null) {
            return;
        }

        if (isInCreativeModeTabBuilding()) {
            return;
        }

        // 检查是否应该跳过替换
        if (ReplacementControl.shouldSkipReplacement()) {
            return;
        }

        String originItemId = Utils.getItemRegistryName(this.item.value());
        String targetItemId = ItemReplacementCache.matchItem(originItemId);

        if (targetItemId != null) {
            Item newItem = Utils.getItemById(targetItemId);
            if (newItem != null) {
                DataComponentPatch currentPatch = this.components.asPatch();

                this.item = newItem.builtInRegistryHolder();

                this.components = PatchedDataComponentMap.fromPatch(newItem.components(), currentPatch);

                ((ItemStack) (Object) this).applyComponentsAndValidate(currentPatch);


                Oneenoughitem.LOGGER.debug("Successfully replaced item {} with {}", originItemId, targetItemId);
            } else {
                Oneenoughitem.LOGGER.warn("Target item not found: {}", targetItemId);
            }
        }
    }

    @Inject(method = "is(Ljava/util/function/Predicate;)Z", at = @At("HEAD"), cancellable = true)
    private void extend(Predicate<Holder<Item>> predicate, CallbackInfoReturnable<Boolean> cir) {
        if (!OEIConfig.get().deeperReplace()) return;
        if (!predicate.test(getItem().builtInRegistryHolder())) {
            String itemId = Utils.getItemRegistryName(item.value());

            boolean matched = false;

            for (Item matchItem : ItemReplacementCache.trackSourceOf(itemId)) {
                if (predicate.test(matchItem.builtInRegistryHolder())) matched = true;
            }
            cir.setReturnValue(matched);
        }
    }

    private boolean isInCreativeModeTabBuilding() {
        return ClientContext.isBuilding();
    }
}
