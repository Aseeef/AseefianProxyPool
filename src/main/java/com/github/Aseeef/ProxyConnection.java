package com.github.Aseeef;

import com.github.Aseeef.http.ApacheHTTPUtil;
import com.github.Aseeef.http.HTTPProxyRequestBuilder;
import com.github.Aseeef.wrappers.ProxyHealthReport;
import com.github.Aseeef.wrappers.ProxySocketAddress;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.Map;

public class ProxyConnection extends Proxy implements Closeable {

    /**
     * The pool this connection was taken from
     */
    private final AseefianProxyPool pool;
    private final ProxySocketAddress proxy;

    public ProxyConnection(AseefianProxyPool pool, ProxySocketAddress proxy) {
        super(proxy.getType(), new InetSocketAddress(proxy.getHost(), proxy.getPort()));
        this.pool = pool;
        this.proxy = proxy;
    }

    public HttpURLConnection getHTTPConnection(String url) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection(this);
    }

    public HTTPProxyRequestBuilder getRequestBuilder(String url) throws MalformedURLException {
        return HTTPProxyRequestBuilder.builder(url).setProxyConnection(this);
    }

    public HttpClientBuilder getApacheClientBuilder() {
        return ApacheHTTPUtil.getBuilder(this);
    }

    public ProxyHealthReport getLatestHealthReport() {
        return pool.proxies.get(proxy).getLatestHealthReport();
    }

    public String getHost() {
        return proxy.getHost();
    }

    public ProxySocketAddress getProxyAddress() {
        return proxy;
    }

    public Map<String, Object> getMetadata() {
        return pool.proxies.get(proxy).getMetadata();
    }

    /**
     * Set whether to skip the leak test for this specific proxy connection.
     * This option will have no affect if the pool config has leak tests disabled.
     * @param enable true if to skip the leak test, false otherwise
     */
    public void skipLeakTest(boolean enable) {
        pool.proxies.get(proxy).setSkipLeakTest(enable);
    }

    @Override
    public synchronized void close() {
        assert !pool.proxies.get(proxy).isInPool() : "Error. The proxy was already in the pool. This should never happen!";
        pool.proxies.get(proxy).setInPool(true);
        pool.proxies.get(proxy).setStackBorrower(null);
        pool.proxies.get(proxy).setSkipLeakTest(false);
        if (pool.proxies.get(proxy).isLeaked()) {
            pool.proxies.get(proxy).setLeaked(false);
            System.err.println("A previously leaked proxy was just returned to the pool!");
        }
    }

}
