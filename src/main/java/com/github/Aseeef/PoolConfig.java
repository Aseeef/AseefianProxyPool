package com.github.Aseeef;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter @Setter @Accessors(chain = true)
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
     * The minimum millis the proxy must have been tested ago to be applicable for a lease.
     */
    private int minMillisTestAgo = 1000 * 15; // 10 seconds
    /**
     * The maximum amount of time to wait for a proxy to respond.
     */
    private int proxyTimeoutMillis = 1000 * 4; // 4 seconds

    /**
     * Whether to allow the leasing of untested proxies. When this is disabled,
     * all proxies obtained via getConnection() will have been tested to be working fine!
     */

}
