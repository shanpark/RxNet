package com.hansdesk.rxnet.tcp;

import com.hansdesk.rxnet.util.Functions;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

public class ServerChannel {

    public static class Builder {
        private String hostname;
        private int port;

        private ChannelSource channelSource;

        public Builder channelSource(ChannelSource channelSource) {
            this.channelSource = channelSource;
            return this;
        }

        public ServerChannel build() {
            return null;
        }
    }

    private final ChannelSource channelSource;
    private final PublishSubject<SocketChannel> subject;
    private Disposable disposable;

    private ServerSocketChannel channel;

    public static ServerChannel create() {
        return new ServerChannel(ChannelSource.single());
    }

    private ServerChannel(ChannelSource source) {
        channelSource = source;
        subject = PublishSubject.create();
    }

    public void start(int port, Consumer<Channel> onAccept) {
        start(new InetSocketAddress(port), onAccept, Functions.ON_ERROR_NOT_IMPL, Functions.EMPTY_RUNNABLE);
    }

    public void start(int port, Consumer<Channel> onAccept, Consumer<Throwable> onError) {
        start(new InetSocketAddress(port), onAccept, onError, Functions.EMPTY_RUNNABLE);
    }

    public void start(int port, Consumer<Channel> onAccept, Consumer<Throwable> onError, Runnable onComplete) {
        start(new InetSocketAddress(port), onAccept, onError, onComplete);
    }

    public void start(String hostname, int port, Consumer<Channel> onAccept) {
        start(new InetSocketAddress(hostname, port), onAccept, Functions.ON_ERROR_NOT_IMPL, Functions.EMPTY_RUNNABLE);
    }

    public void start(String hostname, int port, Consumer<Channel> onAccept, Consumer<Throwable> onError) {
        start(new InetSocketAddress(hostname, port), onAccept, onError, Functions.EMPTY_RUNNABLE);
    }

    public void start(String hostname, int port, Consumer<Channel> onAccept, Consumer<Throwable> onError, Runnable onComplete) {
        start(new InetSocketAddress(hostname, port), onAccept, onError, onComplete);
    }

    /**
     * Start listening and accepting clients.
     *
     * @param inetSocketAddress address to bind.
     * @param onAccept consumer for accepted channels.
     * @param onError consumer for error.
     * @param onComplete action for completion.
     */
    public void start(InetSocketAddress inetSocketAddress, Consumer<Channel> onAccept, Consumer<Throwable> onError, Runnable onComplete) {
        Observer<SocketChannel> observer = new Observer<SocketChannel>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                disposable = d;
                try {
                    channel = ServerSocketChannel.open();
                    channel.configureBlocking(false);
                    channelSource.register(channel, SelectionKey.OP_ACCEPT, subject);
                    channel.bind(inetSocketAddress);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onNext(@NonNull SocketChannel channel) {
                try {
                    onAccept.accept(Channel.from(channelSource, channel));
                } catch (IOException e) {
                    onError.accept(e);
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                onError.accept(e);
            }

            @Override
            public void onComplete() {
                onComplete.run();
            }
        };

        subject.subscribe(observer);
    }

    /**
     * stop listening.
     */
    public void stop() {
        if ((disposable != null) && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    public boolean isRunning() {
        return !disposable.isDisposed();
    }
}
