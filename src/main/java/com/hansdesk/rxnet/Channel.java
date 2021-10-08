package com.hansdesk.rxnet;

public interface Channel {
    void write(Buffer buffer);
    void stop();
}
