package cc.sfclub.sfcraft.memo.auth;

import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;

import java.net.Proxy;

public class AnotherSessionService extends YggdrasilMinecraftSessionService {
    public AnotherSessionService(ServicesKeySet servicesKeySet, Proxy proxy, Environment env) {
        super(servicesKeySet, proxy, env);
    }
}
