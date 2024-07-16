package cc.sfclub.sfcraft.memo.mixin;


import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "startServer",
            at = @At(value = "INVOKE", target = "Ljava/lang/Thread;setUncaughtExceptionHandler(Ljava/lang/Thread$UncaughtExceptionHandler;)V", shift = At.Shift.AFTER)
            , locals = LocalCapture.CAPTURE_FAILHARD)
    private static void adjustThreadPriority(
            Function<Thread, ? extends MinecraftServer> serverFactory,
            CallbackInfoReturnable<? extends MinecraftServer> cir,
            AtomicReference<? extends MinecraftServer> ref,
            Thread thrd) {
        thrd.setPriority(Thread.NORM_PRIORITY + 2); // add priority to main thread
    }
}
