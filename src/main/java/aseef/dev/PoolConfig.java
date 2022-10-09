package aseef.dev;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PoolConfig {

    /**
     * The maximum amount of time a proxy may remain outside the pool in milliseconds
     */
    private long connectionLeakThreshold = 1000L * 10; // 10 seconds
    /**
     * Whether to periodically test proxies to ensure they are still alive.
     */
    private boolean testProxies = true;
    /**
     * The frequency at which to ping each proxy.
     */
    private long testFreqMillis = 1000L * 10; // 10 seconds
    /**
     * The maximum amount of time to wait for a proxy to respond.
     */
    private long proxyTimeoutMillis = 1000L * 8; // 8 seconds

}
