package com.taobao.arthas.grpcweb.grpc.service;

import arthas.grpc.api.ArthasService.ResponseBody;
import arthas.grpc.api.PwdGrpc;
import com.google.protobuf.Empty;
import com.taobao.arthas.core.command.model.PwdModel;

import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.grpcweb.grpc.observer.impl.ArthasStreamObserverImpl;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.lang.instrument.Instrumentation;


public class PwdCommandService extends PwdGrpc.PwdImplBase{

    private GrpcJobController grpcJobController;

    private Instrumentation instrumentation;


    public PwdCommandService(GrpcJobController grpcJobController) {
        this.instrumentation = grpcJobController.getInstrumentation();
        this.grpcJobController = grpcJobController;
    }

    @Override
    public void pwd(Empty empty, StreamObserver<ResponseBody> responseObserver){
        String path = new File("").getAbsolutePath();
        ArthasStreamObserver<ResponseBody> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver, null,grpcJobController);
        arthasStreamObserver.appendResult(new PwdModel(path));
        arthasStreamObserver.onCompleted();
    }
}
