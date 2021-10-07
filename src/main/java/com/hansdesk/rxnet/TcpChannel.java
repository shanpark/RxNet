package com.hansdesk.rxnet;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class TcpChannel implements Channel, Selectable {
    private final PublishSubject<Integer> subject = PublishSubject.create();
    private Disposable disposable;

    private final SocketChannel channel;
    private SelectionKey selectionKey;
    private final Buffer inBuffer = new Buffer(1024);

    public TcpChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public Subject<Integer> subject() {
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
    public void write(Object obj) {
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

    void start(SignalSource source) {
        Observer<Integer> observer = new Observer<Integer>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                disposable = d;

                try {
                    channel.configureBlocking(false);
                    source.register(TcpChannel.this, SelectionKey.OP_READ, subject);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onNext(@NonNull Integer signal) {
                System.out.format("%s\t: signal - %x\n", Thread.currentThread().getName(), signal);
                if ((signal & SelectionKey.OP_READ) > 0) {
                    try {
                        while (true) {
                            int read = channel.read(inBuffer.toByteBuffer());
                            if (read < 0) {
                                channel.close(); // TODO 채널 정리해야 함.
                                break;
                            } else if (read > 0) {
                                if (inBuffer.written(read)) // new buffer allocated
                                    break; // read stop
                            } else {
                                break;
                            }
                        }

                        write(inBuffer);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                if ((signal & SelectionKey.OP_WRITE) > 0) {
                    try {
                        // 모든 데이터를 다 보낼때 까지 write를 시도한다.
                        // 만약 요청된 데이터보다 작게 write를 하고 돌아왔다면 writable하지 않은 상황이다.
                        // 이 경우 OP_WRITE를 켜놓은 채로 리턴한다.
                        // 모든 데이터를 write했다면 OP_WRITE를 꺼야한다.

                        byte[] buf = {'a', 'b', 'c', 'd'};
                        int written = channel.write(ByteBuffer.wrap(buf));

                        // OP_WRITE 끄기.
                        selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
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

    private void clear() {
        try {
            channel.close(); // close server socket channel.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
