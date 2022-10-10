package com.github.Aseeef.exceptions;

public class ProxyConnectionLeakedException extends RuntimeException{
    public ProxyConnectionLeakedException(String s, StackTraceElement[] elements) {
        super(s);
        this.setStackTrace(elements);
    }
}
