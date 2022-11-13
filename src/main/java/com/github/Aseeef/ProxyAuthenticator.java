package com.github.Aseeef;

import com.github.Aseeef.wrappers.ProxyCredentials;
import com.github.Aseeef.wrappers.ProxySocketAddress;

import java.net.*;

public class ProxyAuthenticator extends Authenticator {

    private final AseefianProxyPool pool;
    public ProxyAuthenticator(AseefianProxyPool pool) {
        this.pool = pool;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        Proxy.Type type;
        if (getRequestingProtocol().equals("http")) {
            type = Proxy.Type.HTTP;
        } else if (getRequestingProtocol().startsWith("SOCKS")) {
            type = Proxy.Type.SOCKS;
        } else {
            throw new IllegalArgumentException("Unknown proxy protocol: " + getRequestingProtocol());
        }
        ProxySocketAddress address = new ProxySocketAddress(getRequestingHost(), getRequestingPort(), type);
        ProxyCredentials creds = pool.proxies.get(address).getCredentials().orElse(null);
        if (creds == null)
            return null;
        return new PasswordAuthentication(creds.getUsername(), creds.getPassword().toCharArray());
    }

}
