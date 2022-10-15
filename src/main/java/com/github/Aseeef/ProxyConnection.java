package com.github.Aseeef;

import com.github.Aseeef.proxy.ProxyHealthReport;
import com.github.Aseeef.proxy.ProxySocketAddress;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

public class ProxyConnection extends Proxy implements Closeable {

    /**
     * The pool this connection was taken from
     */
    private final ApacheProxyPool pool;
    private final ProxySocketAddress proxy;

    public ProxyConnection(ApacheProxyPool pool, ProxySocketAddress proxy) {
        super(proxy.getType(), new InetSocketAddress(proxy.getHost(), proxy.getPort()));
        this.pool = pool;
        this.proxy = proxy;
    }

    public URLConnection connect(String url) throws IOException {
        return new URL(url).openConnection(this);
    }

    public ProxyHealthReport getLatestHealthReport() {
        return pool.proxies.get(proxy).getLatestHealthReport();
    }

    public String getHost() {
        return proxy.getHost();
    }

    public Map<String, Object> getMetadata() {
        return pool.proxies.get(proxy).getMetadata();
    }

    @Override
    public synchronized void close() {
        assert !pool.proxies.get(proxy).isInPool() : "Error. The proxy was already in the pool. This should never happen!";
        pool.proxies.get(proxy).setTimeTaken(-1);
        pool.proxies.get(proxy).setStackBorrower(null);
        if (pool.proxies.get(proxy).isLeaked()) {
            pool.proxies.get(proxy).setLeaked(false);
            System.err.println("A previously leaked proxy was just returned to the pool!");
        }
    }

}
