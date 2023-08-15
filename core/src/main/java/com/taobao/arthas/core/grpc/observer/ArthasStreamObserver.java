package com.taobao.arthas.core.grpc.observer;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.model.ResultModel;

import java.lang.instrument.ClassFileTransformer;
import java.util.concurrent.atomic.AtomicInteger;

public interface ArthasStreamObserver<T>  {

    void onNext(T value);

    void onError(Throwable t);

    void onCompleted();

    ArthasStreamObserver write(String msg);

    void appendResult(ResultModel result);

    AtomicInteger times();

    void register(AdviceListener listener, ClassFileTransformer transformer);

    void unregister();

    void end();
    /**
     * End the process.
     *
     * @param status the exit status.
     */
    void end(int status);
    /**
     * End the process.
     *
     * @param status the exit status.
     */
    void end(int status, String message);

}
