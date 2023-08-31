package com.taobao.arthas.core.grpc.view;

import com.google.protobuf.Any;
import com.taobao.arthas.core.AutoGrpc.ResponseBody;
import com.taobao.arthas.core.AutoGrpc.StringValue;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;

public class GrpcMessageView extends GrpcResultView<MessageModel> {
    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, MessageModel result) {
        ResponseBody responseBody  = ResponseBody.newBuilder()
                .setSessionId(arthasStreamObserver.session().getSessionId())
                .setStatusCode(0)
                .setMessage(result.getMessage())
                .build();
        arthasStreamObserver.onNext(responseBody);
    }
}
