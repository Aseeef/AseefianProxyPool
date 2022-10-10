package com.github.Aseeef;

import com.github.Aseeef.proxy.ProxyMeta;
import com.github.Aseeef.proxy.ProxySocketAddress;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public class ProxyConnection extends Proxy implements Closeable {

    /**
     * The pool this connection was taken from
     */
    private final ApacheProxyPool pool;
    private final ProxySocketAddress proxy;
    private final ProxyMeta meta;

    public ProxyConnection(ApacheProxyPool pool, ProxySocketAddress proxy) {
        super(proxy.getType(), new InetSocketAddress(proxy.getHost(), proxy.getPort()));
        this.pool = pool;
        this.proxy = proxy;
        this.meta = pool.proxies.get(proxy);;
    }

    public URLConnection connect(String url) throws IOException {
        return new URL(url).openConnection(this);
    }

    public long getLastInspected() {
        return meta.getLastInspected();
    }

    public String getHost() {
        return proxy.getHost();
    }

    @Override
    public void close() {
        assert !meta.isInPool() : "Error. The proxy was already in the pool. This should never happen!";
        System.out.println("[Debug] Returned back to the pool with in " + (System.currentTimeMillis() - meta.getTimeTaken()) + "ms - " + meta.getTimeTaken());
        meta.setTimeTaken(-1);
        meta.setStackBorrower(null);
        if (meta.isLeaked()) {
            System.err.println("A previously leaked proxy was just returned to the pool!");
        }
    }

}
