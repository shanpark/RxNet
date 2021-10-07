package com.hansdesk.rxnet;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class Channels {
    public static TcpChannel tcpChannelFrom(SocketChannel socketChannel) throws IOException {
        return new TcpChannel(socketChannel);
    }
}
