package com.taobao.arthas.core.grpc.service;

import com.google.protobuf.Empty;
import com.taobao.arthas.core.AutoGrpc.PwdGrpc;
import com.taobao.arthas.core.AutoGrpc.ResponseBody;
import com.taobao.arthas.core.command.model.PwdModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.observer.impl.ArthasStreamObserverImpl;
import com.taobao.arthas.core.shell.session.SessionManager;
import io.grpc.stub.StreamObserver;

import java.io.File;


public class PwdCommandService extends PwdGrpc.PwdImplBase{

    private SessionManager sessionManager;

    private GrpcJobController grpcJobController;

    public PwdCommandService(SessionManager sessionManager,GrpcJobController grpcJobController) {
        this.sessionManager = sessionManager;
        this.grpcJobController = grpcJobController;
    }

    @Override
    public void pwd(Empty empty, StreamObserver<ResponseBody> responseObserver){
        String path = new File("").getAbsolutePath();
        ArthasStreamObserver<ResponseBody> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver, null, sessionManager,grpcJobController);
        arthasStreamObserver.appendResult(new PwdModel(path));
        arthasStreamObserver.onCompleted();
    }
}
