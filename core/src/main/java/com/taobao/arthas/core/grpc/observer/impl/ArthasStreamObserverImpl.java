package com.taobao.arthas.core.grpc.observer.impl;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.shell.system.Process;
import com.taobao.arthas.core.shell.system.ProcessAware;
import io.grpc.stub.StreamObserver;

import java.lang.instrument.ClassFileTransformer;
import java.util.concurrent.atomic.AtomicInteger;

public class ArthasStreamObserverImpl<T> implements ArthasStreamObserver<T> {

    private StreamObserver<T> streamObserver;

    private AtomicInteger times = new AtomicInteger();

    private Process process;

    private AdviceListener listener = null;

    private ClassFileTransformer transformer;


    public ArthasStreamObserverImpl(StreamObserver<T> streamObserver){
        this.streamObserver = streamObserver;
    }

    @Override
    public void onNext(T value) {
        streamObserver.onNext(value);
    }

    @Override
    public void onError(Throwable t) {
        streamObserver.onError(t);
    }

    @Override
    public void onCompleted() {
        streamObserver.onCompleted();
    }

    @Override
    public AtomicInteger times() {
        return times;
    }

    @Override
    public void register(AdviceListener adviceListener, ClassFileTransformer transformer) {
        if (adviceListener instanceof ProcessAware) {
            ProcessAware processAware = (ProcessAware) adviceListener;
            // listener 有可能是其它 command 创建的
            if(processAware.getProcess() == null) {
                processAware.setProcess(this.process);
            }
        }
        this.listener = adviceListener;
        AdviceWeaver.reg(listener);

        this.transformer = transformer;
    }
}
