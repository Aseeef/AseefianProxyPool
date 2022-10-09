package aseef.dev;

import aseef.dev.proxy.ProxyMeta;
import aseef.dev.proxy.ProxySocketAddress;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;

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

    @Override
    public void close() throws IOException {
        ProxyMeta meta = pool.proxies.get(proxy);
        meta.setTimeTaken(-1);
        if (meta.isLeaked()) {
            System.err.println("A previously leaked proxy was just returned to the pool!");
        }
    }

}
