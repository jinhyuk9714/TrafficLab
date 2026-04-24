package com.trafficlab.reservation;

import java.time.Duration;

public interface DistributedLockClient {
    boolean tryLock(String key, String token, Duration ttl);

    void releaseIfOwner(String key, String token);
}
