package com.hansdesk.rxnet;

public interface ServerHandler {
    default void onStart(Server server) {};
    default void onNewChannel(Server server, Channel channel) {
        channel.handlerChain(server.defaultHandlerChain());
    };
    default void onStop(Server server) {};
    default void onError(Server server, Throwable e) {};
}
