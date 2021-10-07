package com.hansdesk.rxnet;

public class Servers {
    public static Server newTcpServer() {
        return new TcpServer();
    }
}
