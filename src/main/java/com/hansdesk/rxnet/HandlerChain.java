package com.hansdesk.rxnet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class HandlerChain implements Iterable<Handler> {
    private final List<Handler> handlers = new ArrayList<>();

    public static HandlerChain of(Handler... handlers) {
        HandlerChain handlerChain = new HandlerChain();
        handlerChain.appendAll(handlers);
        return handlerChain;
    }

    public void append(Handler handler) {
        handlers.add(handler);
    }

    public void appendAll(Handler[] handlers) {
        Collections.addAll(this.handlers, handlers);
    }

    public void insert(int at, Handler handler) {
        handlers.add(at, handler);
    }

    public void remove(int at) {
        handlers.remove(at);
    }

    public Handler get(int at) {
        return handlers.get(at);
    }

    public Iterator<Handler> iterator() {
        return handlers.iterator();
    }
}
