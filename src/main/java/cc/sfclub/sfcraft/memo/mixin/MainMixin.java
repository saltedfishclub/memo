package cc.sfclub.sfcraft.memo.mixin;

import cc.sfclub.sfcraft.memo.Memo;
import cc.sfclub.sfcraft.memo.auth.AnotherSessionService;
import cc.sfclub.sfcraft.memo.auth.FallbackGameProfileRepository;
import cc.sfclub.sfcraft.memo.auth.FallbackMinecraftSessionService;
import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilGameProfileRepository;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import net.minecraft.server.Main;
import net.minecraft.util.ApiServices;
import net.minecraft.util.UserCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;

@Mixin(Main.class)
public abstract class MainMixin {
    @Redirect(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ApiServices;create(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Ljava/io/File;)Lnet/minecraft/util/ApiServices;"))
    private static ApiServices memo$createService(YggdrasilAuthenticationService authenticationService, File rootDirectory) {
        var config = Memo.getInstance().getConfig();
        if (config.alternativeServicesHost != null && config.alternativeSessionHost != null) {
            var env = new Environment(
                    config.alternativeSessionHost,
                    config.alternativeServicesHost,
                    "PROD"
            );
            var gameProfileRepo = new FallbackGameProfileRepository(
                    new YggdrasilGameProfileRepository(authenticationService.getProxy(), env),
                    authenticationService.createProfileRepository()
            );
            return new ApiServices(
                    new FallbackMinecraftSessionService(
                            new AnotherSessionService(
                                    authenticationService.getServicesKeySet(),
                                    authenticationService.getProxy(),
                                    env
                            ),
                            authenticationService.createMinecraftSessionService()
                    ),
                    authenticationService.getServicesKeySet(),
                    gameProfileRepo,
                    new UserCache(
                            gameProfileRepo,
                            new File(rootDirectory, config.useIndependentUserCache
                                    ? "usercache.memo.json"
                                    : "usercache.json")
                    )
            );
        }
        return ApiServices.create(authenticationService, rootDirectory);
    }
}
