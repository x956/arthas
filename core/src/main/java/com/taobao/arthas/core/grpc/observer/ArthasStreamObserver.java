package com.taobao.arthas.core.grpc.observer;

import com.taobao.arthas.core.advisor.AdviceListener;

import java.lang.instrument.ClassFileTransformer;
import java.util.concurrent.atomic.AtomicInteger;

public interface ArthasStreamObserver<T>  {

    void onNext(T value);

    void onError(Throwable t);

    void onCompleted();

    AtomicInteger times();

    void register(AdviceListener listener, ClassFileTransformer transformer);

    void unregister();

    void end();

}
