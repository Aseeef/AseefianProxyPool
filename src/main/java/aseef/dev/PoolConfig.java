package aseef.dev;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PoolConfig {

    /**
     * The maximum amount of time a proxy may remain outside the pool in milliseconds
     */
    private int connectionLeakThreshold = 1000 * 15; // 15 seconds
    /**
     * Whether to periodically test proxies to ensure they are still alive.
     */
    private boolean testProxies = true;
    /**
     * The frequency at which to ping each proxy.
     */
    private int testFreqMillis = 1000 * 5; // 5 seconds
    /**
     * The maximum amount of time to wait for a proxy to respond.
     */
    private int proxyTimeoutMillis = 1000 * 8; // 8 seconds

    /**
     * Whether to allow the leasing of untested proxies. When this is disabled,
     * all proxies obtained via getConnection() will have been tested to be working fine!
     */

}
