package cc.sfclub.sfcraft.memo.auth;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.*;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import lombok.extern.log4j.Log4j2;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log4j2
public class FallbackMinecraftSessionService implements MinecraftSessionService {
    private final MinecraftSessionService[] services;
    private final Map<GameProfile, MinecraftSessionService> pinnedServices = new HashMap<>();

    public FallbackMinecraftSessionService(MinecraftSessionService... services) {
        this.services = services;
    }

    @Override
    public void joinServer(UUID profileId, String authenticationToken, String serverId) throws AuthenticationException {
        int i = 0;
        for (MinecraftSessionService service : services) {
            try {
                service.joinServer(profileId, authenticationToken, serverId);
                return;
            } catch (AuthenticationUnavailableException e) {
                log.warn("Failed to joinServer with session service #" + i, e);
            }
            i++;
        }
        throw new AuthenticationException("No session services available");
    }

    @Override
    public ProfileResult hasJoinedServer(String profileName, String serverId, InetAddress address) throws AuthenticationUnavailableException {
        int i = 0;
        for (MinecraftSessionService service : services) {
            try {
                log.info("Checking player {} with service {}", profileName, service.toString());
                var profile = service.hasJoinedServer(profileName, serverId, address);
                if (profile == null) {
                    return profile;
                }
                if (profile.profile() != null) {
                    pinnedServices.put(profile.profile(), service);
                }
                return profile;
            } catch (AuthenticationUnavailableException exception) {
                log.warn("Failed to check join state with session service #" + i);
                i++;
            }
        }
        throw new AuthenticationUnavailableException("No session services available");
    }

    @Override
    public Property getPackedTextures(GameProfile profile) {
        if (services.length == 0) {
            throw new IllegalStateException("No session services available");
        }
        return pinnedServices.getOrDefault(profile, services[0]).getPackedTextures(profile);
    }

    @Override
    public MinecraftProfileTextures unpackTextures(Property packedTextures) {
        for (MinecraftSessionService service : services) {
            var r = service.unpackTextures(packedTextures);
            if (r != MinecraftProfileTextures.EMPTY) {
                return r;
            }
        }
        return MinecraftProfileTextures.EMPTY;
    }

    @Override
    public ProfileResult fetchProfile(UUID profileId, boolean requireSecure) {
        int i = 0;
        for (MinecraftSessionService service : services) {
            try {
                var r = service.fetchProfile(profileId, requireSecure);
                if (r != null) {
                    return r;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch profile with session service #" + i, e);
            }
        }
        return null;
    }

    @Override
    public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException {
        int i = 0;
        for (MinecraftSessionService service : services) {
            try {
                var r = service.getSecurePropertyValue(property);
                if (r != null) {
                    return r;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch profile property value with session service #" + i, e);
            }
        }
        throw new InsecurePublicKeyException.MissingException("No session services available. Check the logs above.");
    }
}
