package com.taobao.arthas.core.grpc.service;

import com.google.protobuf.Empty;
import com.taobao.arthas.core.AutoGrpc.Properties;
import com.taobao.arthas.core.AutoGrpc.StringKey;
import com.taobao.arthas.core.AutoGrpc.StringValue;
import com.taobao.arthas.core.AutoGrpc.SystemPropertyGrpc;
import com.taobao.arthas.core.command.model.SystemPropertyModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.observer.impl.ArthasStreamObserverImpl;
import io.grpc.stub.StreamObserver;

import java.util.Map;

public class SystemPropertyCommandService extends SystemPropertyGrpc.SystemPropertyImplBase{


    @Override
    public void get(Empty empty, StreamObserver<StringValue> responseObserver){
        ArthasStreamObserver<StringValue> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver);
        arthasStreamObserver.appendResult(new SystemPropertyModel(System.getProperties()));
        arthasStreamObserver.end();
    }

    @Override
    public void getByKey(StringKey request, StreamObserver<StringValue> responseObserver){
        String propertyName = request.getKey();
        ArthasStreamObserver<StringValue> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver);
        // view the specified system property
        String value = System.getProperty(propertyName);
        if (value == null) {
            arthasStreamObserver.end(1, "There is no property with the key " + propertyName);
            return;
        } else {
            arthasStreamObserver.appendResult(new SystemPropertyModel(propertyName, value));
            arthasStreamObserver.end();
        }
    }

    @Override
    public void update(Properties request, StreamObserver<StringValue> responseObserver){
        // get properties from client
        Map<String, String> properties = request.getPropertiesMap();
        String propertyName = "";
        String propertyValue = "";
        // change system property
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            propertyName = entry.getKey();
            propertyValue = entry.getValue();
        }
        ArthasStreamObserver<StringValue> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver);
        try {
            System.setProperty(propertyName, propertyValue);
            arthasStreamObserver.appendResult(new SystemPropertyModel(propertyName, System.getProperty(propertyName)));
            arthasStreamObserver.onCompleted();
        }catch (Throwable t) {
            arthasStreamObserver.end(-1, "Error during setting system property: " + t.getMessage());
        }
    }


}
