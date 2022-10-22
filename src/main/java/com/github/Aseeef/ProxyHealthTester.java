package com.github.Aseeef;

import com.github.Aseeef.wrappers.InternalProxyMeta;
import com.github.Aseeef.wrappers.ProxySocketAddress;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

public class ProxyHealthTester implements Runnable{

    private final AseefianProxyPool pool;
    private final ProxySocketAddress proxy;
    private final InternalProxyMeta meta;
    protected ProxyHealthTester(AseefianProxyPool pool, ProxySocketAddress proxy, InternalProxyMeta meta) {
        this.pool = pool;
        this.proxy = proxy;
        this.meta = meta;
    }
    @Override
    public void run() {
        // skip proxies not in the pool (but dont skip "dead proxies")
        if (!meta.isInPool())
            return;
        meta.setInspecting(true);
        long ping;
        try (ProxyConnection conn = pool.getConnection(proxy, meta)) {
            // using this ip testing method because
            // 1. amazon stays reliably online
            // 2. multiple servers around the world
            // 3. uses little bandwith
            HttpURLConnection connection = conn.getHTTPConnection("http://checkip.amazonaws.com");
            connection.setConnectTimeout(pool.poolConfig.getProxyTimeoutMillis());
            connection.setInstanceFollowRedirects(false);
            ping = System.currentTimeMillis();
            connection.connect();
            ping = System.currentTimeMillis() - ping;
        } catch (Exception ex) {
            if (!(ex instanceof SocketTimeoutException))
                ex.printStackTrace();
            ping = -1;
        }
        meta.getLatestHealthReport().setLastTested(System.currentTimeMillis());
        meta.getLatestHealthReport().setMillisResponseTime(ping);
        meta.setInspecting(false);
    }
}
