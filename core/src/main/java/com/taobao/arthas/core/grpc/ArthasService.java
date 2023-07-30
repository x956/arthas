package com.taobao.arthas.core.grpc;

import com.google.protobuf.Empty;
import com.taobao.arthas.core.AutoGrpc.ArthasServiceGrpc;
import com.taobao.arthas.core.AutoGrpc.HelloReply;
import com.taobao.arthas.core.AutoGrpc.HelloRequest;
import com.taobao.arthas.core.AutoGrpc.Properties;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;

public class ArthasService extends ArthasServiceGrpc.ArthasServiceImplBase{

    @Override
    public void syspropGet(Empty empty, StreamObserver<Properties> responseObserver){
        Map<String,String> result = new HashMap<>();
        result.put("hello", "world");
        Properties properties = Properties.newBuilder().putProperties("hello","world").build();
        responseObserver.onNext(properties);
        responseObserver.onCompleted();
    }

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String message = "hello this is from arthas server";
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName() + " " + message).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }}
