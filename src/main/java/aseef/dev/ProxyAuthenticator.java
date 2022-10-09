package aseef.dev;

import aseef.dev.proxy.ProxyCredentials;
import aseef.dev.proxy.ProxySocketAddress;

import java.net.*;

public class ProxyAuthenticator extends Authenticator {

    private final ApacheProxyPool pool;
    public ProxyAuthenticator(ApacheProxyPool pool) {
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
        ProxyCredentials creds = pool.proxies.get(address);
        return new PasswordAuthentication(creds.getUsername(), creds.getPassword().toCharArray());
    }

}
