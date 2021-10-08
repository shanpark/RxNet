package com.hansdesk.rxnet;

public class SignalSources {
    private static final SignalSource single = new SignalSource();

    public static SignalSource single() {
        return single;
    }
}
