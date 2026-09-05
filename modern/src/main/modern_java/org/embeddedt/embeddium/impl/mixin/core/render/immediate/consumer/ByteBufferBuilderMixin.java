package org.embeddedt.embeddium.impl.mixin.core.render.immediate.consumer;

//? if >=1.21 {
/*import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ByteBufferBuilder.class)
public class ByteBufferBuilderMixin {
    @Shadow
    private int capacity;

    @ModifyArg(method = "ensureCapacity", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;resize(I)V"))
    private int embeddium$growGeometrically(int vanillaSize) {
        int current = this.capacity;
        int geometric = current + (current >> 1);

        // A buffer this close to Integer.MAX_VALUE can't grow geometrically without overflowing; let vanilla's
        // fixed step take it the rest of the way.
        if (geometric <= current) {
            return vanillaSize;
        }

        return Math.max(vanillaSize, geometric);
    }
}

*///?}
