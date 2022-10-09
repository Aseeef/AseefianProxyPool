package aseef.dev.exceptions;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable err) {
        System.err.println("An uncaught Exception occurred in thread: " + t.getName());
        err.printStackTrace();
    }
}
