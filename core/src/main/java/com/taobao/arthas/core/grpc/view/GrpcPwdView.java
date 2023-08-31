package com.taobao.arthas.core.grpc.view;

import com.google.protobuf.Any;
import com.taobao.arthas.core.AutoGrpc.ResponseBody;
import com.taobao.arthas.core.AutoGrpc.SimpleResponse;
import com.taobao.arthas.core.command.model.PwdModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.shell.system.ExecStatus;

/**
 * @author xuyang 2023/8/15
 */
public class GrpcPwdView extends GrpcResultView<PwdModel> {


    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, PwdModel result) {
        SimpleResponse simpleResponse = SimpleResponse.newBuilder()
                .setJobId(arthasStreamObserver.getJobId())
                .setJobStatus(String.valueOf(ExecStatus.TERMINATED))
                .setType(result.getType())
                .putResults("workingDir", result.getWorkingDir())
                .build();
        Any anyMessage = Any.pack(simpleResponse);

        ResponseBody responseBody  = ResponseBody.newBuilder()
                .setSessionId(arthasStreamObserver.session().getSessionId())
                .setStatusCode(0)
                .setMessage("SUCCEEDED")
                .setBody(anyMessage)
                .build();
        arthasStreamObserver.onNext(responseBody);
    }
}
