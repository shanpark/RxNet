package com.hansdesk.rxnet;

public interface Server {
    Server start();
    void stop();
    boolean isRunning();

    void await();
    boolean await(long millis);
}
