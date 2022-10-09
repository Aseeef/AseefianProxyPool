package aseef.dev;

import aseef.dev.proxy.ProxySocketAddress;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

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

    @Override
    public void close() throws IOException {
        pool.returnProxy(proxy);
    }

}
