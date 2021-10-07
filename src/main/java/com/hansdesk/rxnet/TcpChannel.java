package com.hansdesk.rxnet;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class TcpChannel implements Channel {
    private final PublishSubject<Long> subject = PublishSubject.create();
    private Disposable disposable;

    private final SocketChannel channel;
    private final Buffer buffer = new Buffer(1024);

    public TcpChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public Subject<Long> subject() {
        return subject;
    }

    @Override
    public Channel appendHandler(Handler handler) {
        return null;
    }

    @Override
    public void handlerChain(HandlerChain handlerChain) {

    }

    @Override
    public HandlerChain handlerChain() {
        return null;
    }

    @Override
    public void out(Object obj) {

    }

    @Override
    public void stop() {

    }

    void start(SignalSource source) {
        Observer<Long> observer = new Observer<Long>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                disposable = d;

                try {
                    channel.configureBlocking(false);
                    source.register(channel, SelectionKey.OP_READ | SelectionKey.OP_WRITE, subject);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onNext(@NonNull Long signal) {
                try {
                    while (true) {
                        int read = channel.read(buffer.toByteBuffer()); // TODO should read all data using loop
                        if (read < 0) {
                            channel.close(); // TODO
                            break;
                        } else if (read > 0) {
                            if (buffer.written(read)) // new buffer not allocated
                                break; // stop
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                System.out.println("TcpChannel onError()");
//                serverHandler.onError(TcpServer.this, e);
//                clear();
            }

            @Override
            public void onComplete() {
                System.out.println("TcpChannel onComplete()");
//                serverHandler.onStop(TcpServer.this);
//                future.done();
//                clear();
            }
        };

        subject.subscribe(observer);
    }
}
