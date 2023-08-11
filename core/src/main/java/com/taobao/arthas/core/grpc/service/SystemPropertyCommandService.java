package com.taobao.arthas.core.grpc.service;

import com.google.protobuf.Empty;
import com.taobao.arthas.core.AutoGrpc.Properties;
import com.taobao.arthas.core.AutoGrpc.StringKey;
import com.taobao.arthas.core.AutoGrpc.StringValue;
import com.taobao.arthas.core.AutoGrpc.SystemPropertyGrpc;
import com.taobao.arthas.core.command.basic1000.service.SystemPropertyService;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.observer.impl.ArthasStreamObserverImpl;
import io.grpc.stub.StreamObserver;

import java.util.Map;

public class SystemPropertyCommandService extends SystemPropertyGrpc.SystemPropertyImplBase{

    private SystemPropertyService systemPropertyService = new SystemPropertyService();

    @Override
    public void get(Empty empty, StreamObserver<Properties> responseObserver){
        // 获取 Java 的 Properties 对象
        java.util.Properties javaProperties = systemPropertyService.get();
        // 将 Java 的 Properties 对象转换为 gRPC 的 Properties 消息对象
        Properties.Builder propertiesBuilder = Properties.newBuilder();
        for (Map.Entry<Object, Object> entry : javaProperties.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                propertiesBuilder.putProperties((String) entry.getKey(), (String) entry.getValue());
            }
        }
        Properties properties = propertiesBuilder.build();
        ArthasStreamObserver<Properties> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver);
        arthasStreamObserver.onNext(properties);
        arthasStreamObserver.onCompleted();
    }

    @Override
    public void getByKey(StringKey request, StreamObserver<StringValue> responseObserver){
        String propertyName = request.getKey();
        String value = systemPropertyService.getByKey(propertyName);
        StringValue stringValue = StringValue.newBuilder().setValue(value).build();
        ArthasStreamObserver<StringValue> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver);
        arthasStreamObserver.onNext(stringValue);
        arthasStreamObserver.onCompleted();
    }

    @Override
    public void update(Properties request, StreamObserver<Properties> responseObserver){
        // get properties from client
        Map<String, String> properties = request.getPropertiesMap();
        String propertyName = "";
        String propertyValue = "";
        // change system property
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            propertyName = entry.getKey();
            propertyValue = entry.getValue();
        }
        String returnPropertyName;
        String returnPropertyValue;
        try {
            systemPropertyService.update(propertyName, propertyValue);
            returnPropertyName = propertyName;
            returnPropertyValue = System.getProperty(propertyName);
        } catch (Throwable t){
            returnPropertyName = "错误";
            returnPropertyValue = t.getMessage();
        }
        Properties.Builder propertiesBuilder = Properties.newBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            propertiesBuilder.putProperties(returnPropertyName, returnPropertyValue);
        }
        Properties responseProperties = propertiesBuilder.build();

        ArthasStreamObserver<Properties> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver);
        arthasStreamObserver.onNext(responseProperties);
        arthasStreamObserver.onCompleted();
    }


}
