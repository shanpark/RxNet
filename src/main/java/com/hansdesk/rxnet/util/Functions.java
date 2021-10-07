package com.hansdesk.rxnet.util;

import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException;

import java.util.function.Consumer;

public class Functions {
    public static final Runnable EMPTY_RUNNABLE = () -> {
    };
    public static final Consumer<Throwable> ON_ERROR_NOT_IMPL = (e) -> {
        throw new OnErrorNotImplementedException(e);
    };
}
