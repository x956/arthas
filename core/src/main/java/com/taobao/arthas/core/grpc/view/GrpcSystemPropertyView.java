package com.taobao.arthas.core.grpc.view;

import com.google.protobuf.Any;
import arthas.grpc.api.ArthasService.ResponseBody;
import arthas.grpc.api.ArthasService.SimpleResponse;
import com.taobao.arthas.core.command.model.SystemPropertyModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.shell.system.ExecStatus;

public class GrpcSystemPropertyView extends GrpcResultView<SystemPropertyModel>{

    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, SystemPropertyModel result) {

        SimpleResponse simpleResponse = SimpleResponse.newBuilder()
                .setJobId(arthasStreamObserver.getJobId())
                .setJobStatus(String.valueOf(ExecStatus.TERMINATED))
                .setType(result.getType())
                .putAllResults(result.getProps())
                .build();
        Any anyMessage = Any.pack(simpleResponse);

        ResponseBody responseBody  = ResponseBody.newBuilder()
                .setSessionId(arthasStreamObserver.session().getSessionId())
                .setStatusCode(1)
                .setMessage("SUCCEEDED")
                .setBody(anyMessage)
                .build();
        arthasStreamObserver.onNext(responseBody);
    }
}
