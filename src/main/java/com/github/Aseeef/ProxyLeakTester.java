package com.github.Aseeef;

import com.github.Aseeef.exceptions.ProxyConnectionLeakedException;
import com.github.Aseeef.wrappers.InternalProxyMeta;
import com.github.Aseeef.wrappers.ProxySocketAddress;

import java.util.Map;

public class ProxyLeakTester implements Runnable{
    private final AseefianProxyPool pool;
    protected ProxyLeakTester(AseefianProxyPool pool) {
        this.pool = pool;
    }

    @Override
    public void run() {
        for (Map.Entry<ProxySocketAddress, InternalProxyMeta> set : pool.proxies.entrySet()) {
            // skip leak test if its either in the pool already or its dead
            InternalProxyMeta meta = set.getValue();
            boolean inPool = meta.isInPool();
            long timeTaken = meta.getTimeTaken();
            // skip test for proxies in the pool, dead proxies, proxies with meta "skip leak" and proxies being inspected
            if (inPool || !meta.isAlive() || meta.isInspecting() || meta.isSkipLeakTest()) {
                continue;
            }
            // skip test for already leaked proxies
            if (set.getValue().isLeaked() || set.getValue().getStackBorrower() == null)
                continue;

            if (System.currentTimeMillis() - timeTaken > pool.poolConfig.getConnectionLeakThreshold()) {
                meta.setLeaked(true);
                new ProxyConnectionLeakedException(
                        "Detected a proxy leak for the following proxy: " + set.getKey().toString() + " (leaked since " + (System.currentTimeMillis() - timeTaken) + "ms ago)",
                        meta.getStackBorrower()
                ).printStackTrace();
            }
        }
    }
}
