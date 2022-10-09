package com.github.Aseeef;

import com.github.Aseeef.proxy.ProxyMeta;
import com.github.Aseeef.proxy.ProxySocketAddress;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;

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
        return meta.getLastInspected().get();
    }

    @Override
    public void close() {
        meta.getTimeTaken().set(-1);
        if (meta.getLeaked().get()) {
            System.err.println("A previously leaked proxy was just returned to the pool!");
        }
    }

}
