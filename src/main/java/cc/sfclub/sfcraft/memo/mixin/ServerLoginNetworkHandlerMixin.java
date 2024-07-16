package cc.sfclub.sfcraft.memo.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.encryption.NetworkEncryptionException;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    private static final AtomicInteger NEXT_AUTHENTICATOR_THREAD_ID = new AtomicInteger(0);
    @Shadow
    static final Logger LOGGER = LogUtils.getLogger();
    @Shadow
    private ServerLoginNetworkHandler.State state;
    @Final
    @Shadow
    private byte[] nonce;
    @Shadow
    @Final
    MinecraftServer server;
    @Shadow
    @Final
    ClientConnection connection;
    @Shadow
    String profileName;

    @Shadow
    abstract void startVerify(GameProfile profile);

    /**
     * @author icybear
     * @reason use forkjoin pool instead of new Thread
     */
    @Overwrite
    public void onKey(LoginKeyC2SPacket packet) {
        Validate.validState(state == ServerLoginNetworkHandler.State.KEY, "Unexpected key packet", new Object[0]);

        final String string;
        try {
            PrivateKey privateKey = this.server.getKeyPair().getPrivate();
            if (!packet.verifySignedNonce(nonce, privateKey)) {
                throw new IllegalStateException("Protocol error");
            }

            SecretKey secretKey = packet.decryptSecretKey(privateKey);
            Cipher cipher = NetworkEncryptionUtils.cipherFromKey(2, secretKey);
            Cipher cipher2 = NetworkEncryptionUtils.cipherFromKey(1, secretKey);
            string = (new BigInteger(NetworkEncryptionUtils.computeServerId("", this.server.getKeyPair().getPublic(), secretKey))).toString(16);
            this.state = ServerLoginNetworkHandler.State.AUTHENTICATING;
            this.connection.setupEncryption(cipher, cipher2);
        } catch (NetworkEncryptionException var7) {
            throw new IllegalStateException("Protocol error", var7);
        }
        Thread.ofVirtual()
                .name("User Authenticator #" + NEXT_AUTHENTICATOR_THREAD_ID.incrementAndGet())
                .uncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER))
                .start(() -> {
                    String name = Objects.requireNonNull(profileName, "Player name not initialized");

                    try {
                        var socketAddr = connection.getAddress();
                        InetAddress clientAddress = null;
                        if (server.shouldPreventProxyConnections() && socketAddr instanceof InetSocketAddress inetAddr) {
                            clientAddress = inetAddr.getAddress();
                        }
                        ProfileResult profileResult = server.getSessionService().hasJoinedServer(name, string, clientAddress);
                        if (profileResult != null) {
                            GameProfile gameProfile = profileResult.profile();
                            LOGGER.info("UUID of player {} is {}", gameProfile.getName(), gameProfile.getId());
                            startVerify(gameProfile);
                        } else if (server.isSingleplayer()) {
                            LOGGER.warn("Failed to verify username but will let them in anyway!");
                            startVerify(Uuids.getOfflinePlayerProfile(name));
                        } else {
                            getThis().disconnect(Text.translatable("multiplayer.disconnect.unverified_username"));
                            LOGGER.error("Username '{}' tried to join with an invalid session", name);
                        }
                    } catch (AuthenticationUnavailableException var4) {
                        if (server.isSingleplayer()) {
                            LOGGER.warn("Authentication servers are down but will let them in anyway!");
                            startVerify(Uuids.getOfflinePlayerProfile(name));
                        } else {
                            getThis().disconnect(Text.translatable("multiplayer.disconnect.authservers_down"));
                            LOGGER.error("Couldn't verify username because servers are unavailable");
                        }
                    }
                });
    }

    public ServerLoginNetworkHandler getThis() {
        return (ServerLoginNetworkHandler) (Object) this;
    }
}
