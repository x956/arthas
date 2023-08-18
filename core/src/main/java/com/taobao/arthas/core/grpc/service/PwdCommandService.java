package com.taobao.arthas.core.grpc.service;

import com.google.protobuf.Empty;
import com.taobao.arthas.core.AutoGrpc.PwdGrpc;
import com.taobao.arthas.core.AutoGrpc.StringValue;
import com.taobao.arthas.core.command.model.PwdModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.observer.impl.ArthasStreamObserverImpl;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;
import io.grpc.stub.StreamObserver;

import java.io.File;


public class PwdCommandService extends PwdGrpc.PwdImplBase{

    private JobControllerImpl jobController;

    public PwdCommandService(JobControllerImpl jobController) {
        this.jobController = jobController;
    }

    @Override
    public void pwd(Empty empty, StreamObserver<StringValue> responseObserver){
        String path = new File("").getAbsolutePath();
        ArthasStreamObserver<StringValue> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver, jobController);
        arthasStreamObserver.appendResult(new PwdModel(path));
        arthasStreamObserver.onCompleted();
    }
}
