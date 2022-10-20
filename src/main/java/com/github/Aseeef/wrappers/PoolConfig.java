package com.github.Aseeef.wrappers;

import com.github.Aseeef.AseefianProxyPool;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Comparator;
import java.util.Map;

@Getter @Setter @Accessors(chain = true)
public class PoolConfig {

    /**
     * The maximum amount of time to wait for a connection upon calling {@link AseefianProxyPool#getConnection()} by default.
     * If it takes longer then the time specified here to obtain the connection, then {@link AseefianProxyPool#getConnection()} will throw an error.
     *
     * Set this value to less than or equal to 0 to disable this feature.
     */
    private int defaultConnectionWaitMillis = 1000 * 60; // 60 seconds

    /**
     * The maximum amount of time a proxy may remain outside the pool in milliseconds.
     *
     * Set this value to less than or equal to zero to disable proxy leak detection.
     */
    private int connectionLeakThreshold = 1000 * 30; // 30 seconds

    /**
     * Whether to periodically test proxies to ensure they are still alive.
     */
    private boolean testProxies = true;

    /**
     * How often to test proxies in milliseconds to ensure that they are still working?
     */
    private int proxyTestFrequency = 1000 * 15; // 15 seconds

    /**
     * The maximum amount of time to wait for a proxy to respond in milliseconds
     */
    private int proxyTimeoutMillis = 1000 * 3; // 3 seconds

    /**
     * Whether to allow the leasing of untested proxies. When this is disabled,
     * all proxies obtained via getConnection() will have been tested to be working fine!
     */
    private boolean testedProxiesOnly = true;

    /**
     * The frequency in milliseconds at which to test for leaks in the proxy pool.
     */
    private int leakTestFrequencyMillis = 10; // 10 ms

    /**
     * The maximum number of proxies we may concurrently access.
     *
     * Set this value to less than or equal to zero to disable this feature.
     */
    private int maxConcurrency = 500;

    /**
     * This value is the sorting mode using which getConnection() decides which
     * proxy to return to you from the available pool.
     */
    private SortingMode sortingMode = SortingMode.LATENCY;

    /**
     * The custom proxy sorter. This setting has no affect unless {@link PoolConfig#sortingMode} is set to
     * {@link SortingMode#CUSTOM}. When implementing your comparator, keep in mind that the pool
     * will choose the min element from the sorted list of valid proxies.
     */
    private Comparator<Map.Entry<ProxySocketAddress, InternalProxyMeta>> customProxySorter = null;

    public enum SortingMode {
        /**
         * Sorts based on which proxy was used last. Whichever proxy hasn't been used
         * from the pool the longest is the proxy that is returned. Default sorting mode.
         */
        LAST_USED,
        /**
         * This mode sorts proxies based of which proxy has the lowest latency
         * from this JVM to the closest aws datacenter.
         */
        LATENCY,
        /**
         * If you want to sort the proxy based on your own parameters,
         * use this sorting mode and define a proxy sorting comparator by setting
         * a value for {@link PoolConfig#customProxySorter}.
         */
        CUSTOM,
    }

}
