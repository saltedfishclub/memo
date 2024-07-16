package cc.sfclub.sfcraft.memo.mixin;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import com.mojang.authlib.yggdrasil.response.ErrorResponse;
import lombok.SneakyThrows;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.mojang.authlib.minecraft.client.MinecraftClient.CONNECT_TIMEOUT_MS;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

@Mixin(value = MinecraftClient.class, remap = false)
public abstract class MinecraftClientMixin {
    private static final int MAX_RETRIES = 3;
    @Shadow(remap = false)
    @Final
    private Proxy proxy;
    @Shadow(remap = false)
    @Final
    private String accessToken;
    @Unique
    private HttpClient httpClient;
    @Shadow(remap = false)
    @Final
    private static Logger LOGGER;

    @Shadow(remap = false)
    @Final
    private ObjectMapper objectMapper;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void initHttpClient(CallbackInfo ci) {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
                .proxy(ProxySelector.of((InetSocketAddress) proxy.address()))
                .build();
    }

    /**
     * @author icybear
     * @reason make retrying logic
     */
    @Overwrite(remap = false)
    @SneakyThrows
    public <T> T get(final URL url, final Class<T> responseClass) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(responseClass);
        var req = memo$newReq(url.toURI()).GET().build();
        return memo$makeRequest(req, responseClass);
    }

    /**
     * @author icybear
     * @reason make retrying logic
     */
    @Overwrite(remap = false)
    @SneakyThrows
    public <T> T post(final URL url, final Class<T> responseClass) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(responseClass);
        var req = memo$newReq(url.toURI()).POST(HttpRequest.BodyPublishers.noBody()).build();
        return memo$makeRequest(req, responseClass);
    }

    /**
     * @author icybear
     * @reason make retrying logic
     */
    @Overwrite(remap = false)
    @SneakyThrows
    public <T> T post(final URL url, final Object body, final Class<T> responseClass) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(responseClass);
        Objects.requireNonNull(body);
        var req = memo$newReq(url.toURI())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .header("Content-Type", "application/json; charset=utf-8")
                .build();
        return memo$makeRequest(req, responseClass);
    }

    @Unique
    @SneakyThrows
    private <T> T memo$makeRequest(HttpRequest req, Class<T> responseClass) {
        HttpResponse<String> lastResp = null;
        int retried = 0;
        Exception lastException = null;
        while (retried < MAX_RETRIES) {
            try {
                lastResp = httpClient.send(req, ofString());
                if (lastResp.statusCode() < 400) {
                    return objectMapper.readValue(lastResp.body(), responseClass);
                } else {
                    if (lastResp.body() != null) {
                        lastException = new MinecraftClientHttpException(lastResp.statusCode(), objectMapper.readValue(lastResp.body(), ErrorResponse.class));
                    } else {
                        lastException = new MinecraftClientHttpException(lastResp.statusCode());
                    }
                }
            } catch (Exception e) {
                lastException = e;
                retried++;
                if (retried != MAX_RETRIES) {
                    var sec = Math.pow(2, retried);
                    LOGGER.warn("Failed to request " + req.uri() + ", retrying after " + sec + "s: {}", e.getMessage());
                    TimeUnit.SECONDS.sleep((long) sec);
                }
            }
        }
        if (lastResp != null) {
            throw lastException;  /// not connection error
        } else {
            throw new MinecraftClientException(
                    MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE,
                    "Failed to read from " + req.uri() + " due to " + lastException.getMessage(),
                    lastException
            );
        }
    }

    @Unique
    private HttpRequest.Builder memo$newReq(URI uri) {
        var b = HttpRequest.newBuilder(uri);
        if (accessToken != null) {
            b.header("Authorization", "Bearer " + accessToken);
        }
        return b;
    }
}
