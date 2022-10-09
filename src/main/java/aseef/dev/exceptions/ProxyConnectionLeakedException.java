package aseef.dev.exceptions;

public class ProxyConnectionLeakedException extends RuntimeException{
    public ProxyConnectionLeakedException(String s) {
        super(s);
    }
}
