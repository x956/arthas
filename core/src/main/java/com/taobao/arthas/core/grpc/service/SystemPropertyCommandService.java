package com.taobao.arthas.core.grpc.service;

import arthas.grpc.api.ArthasService.ResponseBody;
import arthas.grpc.api.ArthasService.StringKey;
import arthas.grpc.api.ArthasService.Properties;
import arthas.grpc.api.SystemPropertyGrpc;
import com.google.protobuf.Empty;
import com.taobao.arthas.core.command.model.SystemPropertyModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.observer.impl.ArthasStreamObserverImpl;
import com.taobao.arthas.core.shell.session.SessionManager;
import io.grpc.stub.StreamObserver;

import java.util.Map;

public class SystemPropertyCommandService extends SystemPropertyGrpc.SystemPropertyImplBase{

    private SessionManager sessionManager;

    private GrpcJobController grpcJobController;


    public SystemPropertyCommandService(SessionManager sessionManager, GrpcJobController grpcJobController) {
        this.sessionManager = sessionManager;
        this.grpcJobController = grpcJobController;
    }

    @Override
    public void get(Empty empty, StreamObserver<ResponseBody> responseObserver){
        ArthasStreamObserver<ResponseBody> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver, null, sessionManager, grpcJobController);
        arthasStreamObserver.appendResult(new SystemPropertyModel(System.getProperties()));
        arthasStreamObserver.end();
    }

    @Override
    public void getByKey(StringKey request, StreamObserver<ResponseBody> responseObserver){
        String propertyName = request.getKey();
        ArthasStreamObserver<ResponseBody> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver,null, sessionManager, grpcJobController);
        // view the specified system property
        String value = System.getProperty(propertyName);
        if (value == null) {
            arthasStreamObserver.end(-1, "There is no property with the key " + propertyName);
            return;
        } else {
            arthasStreamObserver.appendResult(new SystemPropertyModel(propertyName, value));
            arthasStreamObserver.end();
        }
    }

    @Override
    public void update(Properties request, StreamObserver<ResponseBody> responseObserver){
        // get properties from client
        Map<String, String> properties = request.getPropertiesMap();
        String propertyName = "";
        String propertyValue = "";
        // change system property
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            propertyName = entry.getKey();
            propertyValue = entry.getValue();
        }
        ArthasStreamObserver<ResponseBody> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver,null, sessionManager, grpcJobController);
        try {
            System.setProperty(propertyName, propertyValue);
            arthasStreamObserver.appendResult(new SystemPropertyModel(propertyName, System.getProperty(propertyName)));
            arthasStreamObserver.onCompleted();
        }catch (Throwable t) {
            arthasStreamObserver.end(-1, "Error during setting system property: " + t.getMessage());
        }
    }
}
