package com.github.Aseeef.http;

import com.github.Aseeef.ProxyConnection;
import com.github.Aseeef.wrappers.ProxySocketAddress;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;

public class ApacheHTTPUtil {

    public static HttpHost toApacheHttpHost(ProxyConnection proxyConnection) {
        String scheme;
        switch (proxyConnection.getProxyAddress().getType()) {
            case HTTP:
                scheme = "http";
                break;
            default:
                throw new IllegalArgumentException("The proxy type " + proxyConnection.getProxyAddress().getType() + " is not supported by Apache HttpClient!");
        }
        return new HttpHost(proxyConnection.getHost(), proxyConnection.getProxyAddress().getPort(), scheme);
    }

    // adapted from https://www.baeldung.com/httpclient-advanced-config
    public static HttpClientBuilder getBuilder(ProxyConnection proxyConnection) {
        HttpHost proxy = toApacheHttpHost(proxyConnection);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

        ProxySocketAddress address = proxyConnection.getProxyAddress();
        HttpClientBuilder builder = HttpClients.custom();
        builder.setRoutePlanner(routePlanner);

        String protocol;
        switch (address.getType()) {
            case HTTP:
                protocol = "http";
                break;
            default:
                throw new IllegalStateException("The proxy type " + address.getType() + " is not supported by Apache HttpClient!");
        }
        PasswordAuthentication authentication = Authenticator.requestPasswordAuthentication(
                address.getHost(),
                new InetSocketAddress(address.getHost(), address.getPort()).getAddress(),
                address.getPort(),
                protocol,
                "basic",
                ""
        );

        // authentication null means no authentication was provided
        if (authentication != null) {
            //Client credentials
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(proxy),
                    new UsernamePasswordCredentials(authentication.getUserName(), new String(authentication.getPassword())));

            // Create AuthCache instance
            AuthCache authCache = new BasicAuthCache();

            BasicScheme basicAuth = new BasicScheme();
            authCache.put(proxy, basicAuth);
            HttpClientContext context = HttpClientContext.create();
            context.setCredentialsProvider(credentialsProvider);
            context.setAuthCache(authCache);

            // set credentials provider
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }

        return builder;
    }

}
