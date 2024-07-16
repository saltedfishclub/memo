package cc.sfclub.sfcraft.memo.mixin;


import cc.sfclub.sfcraft.memo.Memo;
import com.mojang.datafixers.DataFixer;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.ApiServices;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.net.Proxy;
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
