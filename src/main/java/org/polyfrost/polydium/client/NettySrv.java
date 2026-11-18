package org.polyfrost.polydium.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DefaultDnsRecordDecoder;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverTimeoutException;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.FutureListener;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

final class NettySrv {
    private static final int DEFAULT_PORT = 25565;
    private static final long NEGATIVE_TTL_SECONDS = 60;
    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();
    private static DnsNameResolver resolver;

    private record Cached(String host, int port, long expiresAt) {
    }

    private NettySrv() {
    }

    static void init() throws ReflectiveOperationException {
        Field field = Class.forName("dev.rdh.sarcio.util.AsyncServerPinger$Dns").getDeclaredField("RESOLVER");
        field.setAccessible(true);
        resolver = (DnsNameResolver) field.get(null);
    }

    static void resolve(String host, BiConsumer<String, Integer> done) {
        Cached cached = CACHE.get(host);
        if (cached != null && cached.expiresAt - System.nanoTime() > 0) {
            done.accept(cached.host, cached.port);
            return;
        }
        DefaultDnsQuestion question = new DefaultDnsQuestion("_minecraft._tcp." + host, DnsRecordType.SRV);
        resolver.resolveAll(question).addListener((FutureListener<List<DnsRecord>>) future -> {
            String target = host;
            int port = DEFAULT_PORT;
            long ttl = NEGATIVE_TTL_SECONDS;
            try {
                if (future.isSuccess()) {
                    for (DnsRecord record : future.getNow()) {
                        try {
                            if (target == host && record instanceof DnsRawRecord raw) {
                                ByteBuf data = raw.content().duplicate().skipBytes(4);
                                port = data.readUnsignedShort();
                                target = DefaultDnsRecordDecoder.decodeName(data);
                                ttl = record.timeToLive();
                            }
                        } catch (RuntimeException ignored) {
                            target = host;
                            port = DEFAULT_PORT;
                        } finally {
                            ReferenceCountUtil.release(record);
                        }
                    }
                }
                if (future.isSuccess() || !(future.cause() instanceof DnsNameResolverTimeoutException)) {
                    CACHE.put(host, new Cached(target, port, System.nanoTime() + TimeUnit.SECONDS.toNanos(ttl)));
                }
            } finally {
                done.accept(target, port);
            }
        });
    }
}
