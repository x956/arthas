package com.taobao.arthas.core.grpc.service;

import com.google.protobuf.Empty;
import com.taobao.arthas.core.AutoGrpc.PwdGrpc;
import com.taobao.arthas.core.AutoGrpc.StringValue;
import com.taobao.arthas.core.command.basic1000.service.PwdService;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.observer.impl.ArthasStreamObserverImpl;
import io.grpc.stub.StreamObserver;


public class PwdCommandService extends PwdGrpc.PwdImplBase{
    @Override
    public void pwd(Empty empty, StreamObserver<StringValue> responseObserver){
        PwdService pwdService = new PwdService();
        String path = pwdService.pwd();
        StringValue stringValue = StringValue.newBuilder().setValue(path).build();
        ArthasStreamObserver<StringValue> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver);
        arthasStreamObserver.onNext(stringValue);
        arthasStreamObserver.onCompleted();
    }
}
