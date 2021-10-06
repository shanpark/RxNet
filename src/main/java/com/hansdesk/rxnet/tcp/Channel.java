package com.hansdesk.rxnet.tcp;

import io.reactivex.rxjava3.subjects.PublishSubject;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Channel {

    private final PublishSubject<Buffer> subject;
    private SocketChannel channel;

    public static Channel from(ChannelSource channelSource, SocketChannel channel) throws IOException {
        return new Channel(channelSource, channel);
    }

    private Channel(ChannelSource channelSource, SocketChannel socketChannel) throws IOException {
        subject = PublishSubject.create();

        channel = socketChannel;
        channel.configureBlocking(false);
        channelSource.register(channel, SelectionKey.OP_READ, subject);
    }
}
