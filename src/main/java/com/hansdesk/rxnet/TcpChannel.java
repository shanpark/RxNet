package com.hansdesk.rxnet;

import com.hansdesk.rxnet.util.Handlers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class TcpChannel implements Channel, Selectable {
    private final PublishSubject<Integer> subject = PublishSubject.create();
    private Disposable disposable;

    private final SocketChannel channel;
    private final Buffer inBuffer = new Buffer(1024);
    private final Buffer outBuffer = new Buffer(1024);
    private SelectionKey selectionKey;

    private Handler handler = Handlers.EMPTY_CHANNEL_HANDLER;

    public TcpChannel(SocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public void write(Buffer buffer) {
        outBuffer.write(buffer);
        selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
    }

    @Override
    public void stop() {
        if (!disposable.isDisposed()) {
            disposable.dispose(); // subject의 동작을 중지시킨다.
            clear();
        }
    }

    @Override
    public SelectableChannel channel() {
        return channel;
    }

    @Override
    public void selectionKey(SelectionKey key) {
        selectionKey = key;
    }

    public void handler(Handler handler) {
        this.handler = handler;
    }

    void start(SignalSource source) {
        Observer<Integer> observer = new Observer<Integer>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                disposable = d;

                try {
                    channel.configureBlocking(false);
                    source.register(TcpChannel.this, SelectionKey.OP_READ, subject);

                    handler.onStart(TcpChannel.this); // socket의 준비가 끝났으므로 onStart() 호출.
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onNext(@NonNull Integer signal) {
                System.out.format("%s\t: signal - %x\n", Thread.currentThread().getName(), signal);
                try {
                    // readable signal
                    if ((signal & SelectionKey.OP_READ) > 0)
                        handlerRead();

                    // writable signal
                    if ((signal & SelectionKey.OP_WRITE) > 0)
                        handlerWrite();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                System.out.println("TcpChannel onError()");
                handler.onError(TcpChannel.this, e);
                clear();
            }

            @Override
            public void onComplete() {
                System.out.println("TcpChannel onComplete()");
                handler.onStop(TcpChannel.this);
                clear();
            }
        };

        subject.subscribe(observer);
    }

    private void handlerRead() throws IOException {
        while (true) {
            int read = channel.read(inBuffer.byteBufferForWrite());
            if (read < 0) {
                stop();
                break;
            } else if (read > 0) {
                if (!inBuffer.advanceWritePosition(read)) // buffer not changed.
                    break; // read stop
            } else {
                break;
            }
        }

        if (inBuffer.readable()) {
            handler.onInbound(this, inBuffer); // socket 읽어온 데이터가 있으면 handler 호출.
            inBuffer.unmark(); // always unmark.
        }
    }

    private void handlerWrite() throws IOException {
        if (outBuffer.readable())
            handler.onOutbound(this, outBuffer);

        int writable = outBuffer.readableBytes();
        while (writable > 0) { // writable 데이터가 있으면 계속한다.
            int written = channel.write(outBuffer.byteBufferForRead());
            if (written > 0)
                outBuffer.advanceReadPosition(written);

            if (written < writable) { // 요청한만큼 write를 하지못했으면
                break; // 중단 시킨다. write가 가능할 때 즉시 OP_WRITE 신호가 올 것이다.
            } else if (written == writable) { // 요청한 데이터 모두 write를 했으면
                writable = outBuffer.readableBytes(); // writable 데이터가 더 있는 지 체크.
            }
        }

        // 더 이상 write할 데이터가 없으면 OP_WRITE 끄기.
        if (writable <= 0)
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
    }

    private void clear() {
        try {
            channel.close(); // close server socket channel.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
