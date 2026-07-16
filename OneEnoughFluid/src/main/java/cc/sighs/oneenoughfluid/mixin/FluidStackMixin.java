package cc.sighs.oneenoughfluid.mixin;

import cc.sighs.oneenoughfluid.init.FluidReplacementCache;
import cc.sighs.oneenoughitem.util.ReplacementControl;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FluidStack.class)
public abstract class FluidStackMixin {
    @ModifyVariable(
            method = "<init>(Lnet/minecraft/world/level/material/Fluid;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static Fluid oef$replaceConstructorFluid(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return fluid;
        if (ReplacementControl.shouldSkipReplacement()) return fluid;
        try {
            var id = BuiltInRegistries.FLUID.getKey(fluid);
            String targetId = FluidReplacementCache.matchFluid(id.toString());
            if (targetId == null) return fluid;
            Fluid target = BuiltInRegistries.FLUID.getValue(Identifier.parse(targetId));
            if (target != Fluids.EMPTY) {
                return target;
            }
        } catch (Exception ignored) {
        }
        return fluid;
    }
}
