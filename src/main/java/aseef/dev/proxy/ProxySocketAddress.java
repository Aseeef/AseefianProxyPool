package aseef.dev.proxy;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor @Getter @Setter @EqualsAndHashCode
public class ProxySocketAddress {
    private final String host;
    private final int port;
    private final java.net.Proxy.Type type;
}
