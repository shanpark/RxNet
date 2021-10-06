package com.hansdesk.rxnet.tcp;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.Subject;

import java.io.IOException;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ChannelSource {

    private static class ReqRegister {
        public SelectableChannel channel;
        public int ops;
        public Subject<?> subject;

        public ReqRegister(SelectableChannel channel, int ops, Subject<?> subject) {
            this.channel = channel;
            this.ops = ops;
            this.subject = subject;
        }

        public void register(Selector selector) throws ClosedChannelException {
            channel.register(selector, ops, subject);
        }
    }

    private static final ChannelSource single = new ChannelSource();

    private Selector selector;
    private final List<ReqRegister> registerQueue;

    /**
     *
     *
     * @return return single channel source.
     */
    public static ChannelSource single() {
        return single;
    }

    ChannelSource() {
        this(e -> { throw new OnErrorNotImplementedException(e); }, Functions.EMPTY_RUNNABLE);
    }

    ChannelSource(Consumer<Throwable> onError, Runnable onComplete) {
        registerQueue = new ArrayList<>();
        Observable.<SelectionKey>create(emitter -> {
            try {
                selector = Selector.open();
                while (selector.isOpen()) {
                    int count = selector.select();
                    if (count > 0) {
                        final Set<SelectionKey> keys = selector.selectedKeys();
                        for (SelectionKey key : keys)
                            emitter.onNext(key);
                        keys.clear();
                    }

                    if (!registerQueue.isEmpty()) {
                        synchronized (registerQueue) {
                            for (ReqRegister request : registerQueue)
                                request.register(selector);
                            registerQueue.clear();
                        }
                    }
                }

                emitter.onComplete();
            } catch (Throwable e) {
                emitter.onError(e);
            }
        })
        .subscribeOn(Schedulers.io())
        .subscribe(this::onNext, onError::accept, onComplete::run);
    }

    private void onNext(SelectionKey key) throws Throwable {
        if (key.isAcceptable()) {
            SocketChannel accepted = ((ServerSocketChannel) key.channel()).accept();
            //noinspection unchecked
            ((Subject<SocketChannel>) key.attachment()).onNext(accepted);
        }
    }

    /**
     * event 생성을 끝낸다.
     * 한 번 stop하면 다시 시작할 수 있는 방법은 없다.
     */
    public void stop() throws IOException {
        if (selector.isOpen())
            selector.close();
    }

    public void register(SelectableChannel channel, int ops, Subject<?> subject) throws ClosedChannelException {
        synchronized (registerQueue) {
            registerQueue.add(new ReqRegister(channel, ops, subject));
        }
        selector.wakeup();
    }
}
