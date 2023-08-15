package com.taobao.arthas.core.grpc.observer.impl;

import com.taobao.arthas.core.AutoGrpc.StringValue;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.StatusModel;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.distribution.impl.GrpcResultDistributorImpl;
import com.taobao.arthas.core.distribution.impl.TermResultDistributorImpl;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.view.GrpcResultViewResolver;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.ProcessAware;
import io.grpc.stub.StreamObserver;

import java.lang.instrument.ClassFileTransformer;
import java.util.concurrent.atomic.AtomicInteger;

public class ArthasStreamObserverImpl<T> implements ArthasStreamObserver<T> {

    private StreamObserver<T> streamObserver;

    private AtomicInteger times = new AtomicInteger();

    private GrpcProcess process;

    private AdviceListener listener = null;

    private ClassFileTransformer transformer;

    private int jobId;

    private ResultDistributor resultDistributor;


    public ArthasStreamObserverImpl(StreamObserver<T> streamObserver){
        this.streamObserver = streamObserver;
        if (resultDistributor == null) {
            resultDistributor = new GrpcResultDistributorImpl(this, ArthasBootstrap.getInstance().getGrpcResultViewResolver());
        }
        this.process = new GrpcProcess();
        this.process.setProcessStatus(ExecStatus.RUNNING);
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
        this.process.setProcessStatus(ExecStatus.TERMINATED);
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

    @Override
    public void unregister() {
        if (transformer != null) {
            ArthasBootstrap.getInstance().getTransformerManager().removeTransformer(transformer);
        }
        this.process.setProcessStatus(ExecStatus.TERMINATED);
        if (listener instanceof ProcessAware) {
            // listener有可能其它 command 创建的，所以不能unRge
            if (this.process.equals(((ProcessAware) listener).getProcess())) {
                AdviceWeaver.unReg(listener);
            }
        } else {
            AdviceWeaver.unReg(listener);
        }
    }

    @Override
    public void end() {
        end(0);
    }

    @Override
    public void end(int statusCode) {
        end(statusCode, null);
    }

    @Override
    public void end(int statusCode, String message) {
        terminate(statusCode, message);
    }


    private synchronized boolean terminate(int exitCode, String message) {
        boolean flag;
        if (process.status() != ExecStatus.TERMINATED) {
            //add status message
            this.appendResult(new StatusModel(exitCode, message));
            if (process != null) {
                this.unregister();
            }
            flag = true;
        } else {
            flag = false;
        }
        this.onCompleted();
        return flag;
    }

    @Override
    public ArthasStreamObserver write(String msg) {
        StringValue result = StringValue.newBuilder().setValue(msg).build();
        onNext((T) result);
        return this;
    }

    @Override
    public void appendResult(ResultModel result) {
        if (process.status() != ExecStatus.RUNNING) {
            throw new IllegalStateException(
                    "Cannot write to standard output when " + process.status().name().toLowerCase());
        }
        result.setJobId(jobId);
        if (resultDistributor != null) {
            resultDistributor.appendResult(result);
        }
    }
}
