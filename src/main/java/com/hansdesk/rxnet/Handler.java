package com.hansdesk.rxnet;

public interface Handler {
    default boolean onStart(Channel channel) {
        return true;
    }
    default Object onInbound(Channel channel, Object inboundObj) { return null; }
    default Object onOutbound(Channel channel, Object outboundObj) { return outboundObj; }
    default boolean onStop(Channel channel) {
        return true;
    }
    default boolean onError(Channel channel, Throwable e) {
        return true;
    }
}
