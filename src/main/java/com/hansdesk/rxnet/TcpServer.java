package com.hansdesk.rxnet;

import com.hansdesk.rxnet.util.JustFuture;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class TcpServer implements Server, Selectable {
    private final SignalSource source = SelectorSources.single();
    private final PublishSubject<Integer> subject = PublishSubject.create();
    private final JustFuture future = new JustFuture();
    private Disposable disposable;

    private ServerSocketChannel channel;
    private SelectionKey selectionKey;

    private String hostname;
    private int port;
    private ServerHandler serverHandler;

    TcpServer() {
    }

    @Override
    public Server host(String hostname) {
        this.hostname = hostname;
        return this;
    }

    @Override
    public Server port(int port) {
        this.port = port;
        return this;
    }

    @Override
    public Server serverHandler(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
        return this;
    }

    @Override
    public Server defaultHandlerChain(HandlerChain handlerChain) {
        return this;
    }

    @Override
    public HandlerChain defaultHandlerChain() {
        return null;
    }

    @Override
    public Server start() {
        Observer<Integer> observer = new Observer<Integer>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                disposable = d;

                try {
                    InetSocketAddress inetSocketAddress = (hostname == null) ? new InetSocketAddress(port) : new InetSocketAddress(hostname, port);

                    channel = ServerSocketChannel.open();
                    channel.configureBlocking(false);
                    source.register(TcpServer.this, SelectionKey.OP_ACCEPT, subject);
                    channel.bind(inetSocketAddress);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onNext(@NonNull Integer signal) {
                System.out.format("%s\t: signal - %x\n", Thread.currentThread().getName(), signal);
                try {
                    SocketChannel socketChannel = channel.accept();
                    TcpChannel newChannel = Channels.tcpChannelFrom(socketChannel);

                    newChannel.handlerChain(defaultHandlerChain());

                    serverHandler.onNewChannel(TcpServer.this, newChannel);

                    newChannel.start(source);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                System.out.println("TcpServer onError()");
                serverHandler.onError(TcpServer.this, e);
                clear();
            }

            @Override
            public void onComplete() {
                System.out.println("TcpServer onComplete()");
                serverHandler.onStop(TcpServer.this);
                future.done();
                clear();
            }
        };

        subject.subscribe(observer);
        return this;
    }

    @Override
    public void stop() {
        if (isRunning()) {
            disposable.dispose(); // subject의 동작을 중지시킨다.
            clear();
        }
    }

    @Override
    public boolean isRunning() {
        return !future.isDone();
    }

    @Override
    public void await() {
        future.await();
    }

    @Override
    public boolean await(long millis) {
        return future.await(millis);
    }

    @Override
    public SelectableChannel channel() {
        return channel;
    }

    @Override
    public void selectionKey(SelectionKey key) {
        selectionKey = key;
    }

    private void clear() {
        try {
            future.done(); // unlock waiting threads.
            channel.close(); // close server socket channel.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
