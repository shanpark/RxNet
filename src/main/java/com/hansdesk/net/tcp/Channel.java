package com.hansdesk.net.tcp;

import java.nio.channels.SocketChannel;

public class Channel {

    private SocketChannel channel;

    public static Channel from(SocketChannel channel) {
        return new Channel(channel);
    }

    private Channel(SocketChannel socketChannel) {
        this.channel = socketChannel;
    }
}
