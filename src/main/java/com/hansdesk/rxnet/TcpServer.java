package com.hansdesk.rxnet;

import com.hansdesk.rxnet.util.Handlers;
import com.hansdesk.rxnet.util.JustFuture;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class TcpServer implements Server, Selectable {
    private final SignalSource source = SignalSources.single();
    private final Subject<Integer> subject = (Subject<Integer>) PublishSubject.<Integer>create().observeOn(Schedulers.computation());
    private final JustFuture future = new JustFuture();
    private Disposable disposable;

    private ServerSocketChannel channel;
    private SelectionKey selectionKey;

    private String hostname;
    private int port;
    private ServerHandler serverHandler = Handlers.EMPTY_SERVER_HANDLER;
    private Handler channelHandler = Handlers.EMPTY_CHANNEL_HANDLER;

    TcpServer() {
    }

    public TcpServer host(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public TcpServer port(int port) {
        this.port = port;
        return this;
    }

    public TcpServer serverHandler(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
        return this;
    }

    public TcpServer channelHandler(Handler handler) {
        this.channelHandler = handler;
        return this;
    }

    @Override
    public Server start() {
        InetSocketAddress inetSocketAddress = (hostname == null) ? new InetSocketAddress(port) : new InetSocketAddress(hostname, port);

        try {
            // 가장 먼저 subject 구독을 먼저 시작.
            disposable = subject.subscribe(this::onSignal, this::onError, this::onComplete);

            // server socket channel을 생성.
            channel = ServerSocketChannel.open();
            channel.configureBlocking(false);

            // accept event를 받을 수 있게 등록한다.
            source.register(this, SelectionKey.OP_ACCEPT, subject); // source에 자신을 등록.

            // listen을 시작한다.
            channel.bind(inetSocketAddress);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private void onSignal(Integer signal) {
        System.out.format("%s\t: signal - %x\n", Thread.currentThread().getName(), signal);
        try {
            SocketChannel socketChannel = channel.accept();
            TcpChannel newChannel = Channels.tcpChannelFrom(socketChannel);
            newChannel.handler(channelHandler);

            serverHandler.onNewChannel(TcpServer.this, newChannel);

            newChannel.start(source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onError(Throwable e) {
        System.out.println("TcpServer onError()");
        serverHandler.onError(TcpServer.this, e);
        clear();
    }

    private void onComplete() {
        System.out.println("TcpServer onComplete()");
        serverHandler.onStop(TcpServer.this);
        future.done();
        clear();
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
