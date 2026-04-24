package com.trafficlab.support;

import com.trafficlab.reservation.DistributedLockClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDistributedLockClient implements DistributedLockClient {

    private final Map<String, LockValue> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key, String token, Duration ttl) {
        Instant now = Instant.now();
        locks.computeIfPresent(key, (ignored, current) -> current.expiresAt().isBefore(now) ? null : current);
        return locks.putIfAbsent(key, new LockValue(token, now.plus(ttl))) == null;
    }

    @Override
    public void releaseIfOwner(String key, String token) {
        locks.computeIfPresent(key, (ignored, current) -> current.token().equals(token) ? null : current);
    }

    private record LockValue(String token, Instant expiresAt) {
    }
}
