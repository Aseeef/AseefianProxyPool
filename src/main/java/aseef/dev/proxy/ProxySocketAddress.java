package aseef.dev.proxy;

import lombok.*;

@AllArgsConstructor @Getter @Setter @EqualsAndHashCode @ToString
public class ProxySocketAddress {
    private final String host;
    private final int port;
    private final java.net.Proxy.Type type;
}
