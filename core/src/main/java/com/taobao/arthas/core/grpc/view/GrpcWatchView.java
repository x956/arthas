package com.taobao.arthas.core.grpc.view;

import com.google.protobuf.Any;
import com.taobao.arthas.core.AutoGrpc.ResponseBody;
import com.taobao.arthas.core.AutoGrpc.WatchResponse;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * Term view for WatchModel
 *
 * @author xuyang 2023/8/15
 */
public class GrpcWatchView extends GrpcResultView<WatchModel> {

    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, WatchModel model) {
        ObjectVO objectVO = model.getValue();
        String result = StringUtils.objectToString(
                objectVO.needExpand() ? new ObjectView(model.getSizeLimit(), objectVO).draw() : objectVO.getObject());
        WatchResponse watchResponse = WatchResponse.newBuilder()
                .setJobId(model.getJobId())
                .setAccessPoint(model.getAccessPoint())
                .setClassName(model.getClassName())
                .setCost(model.getCost())
                .setMethodName(model.getMethodName())
                .setSizeLimit(model.getSizeLimit())
                .setTs(DateUtils.formatDate(model.getTs()))
                .setType(model.getType())
                .setValue(result)
                .build();
        Any anyMessage = Any.pack(watchResponse);
        ResponseBody responseBody  = ResponseBody.newBuilder()
                .setSessionId(arthasStreamObserver.session().getSessionId())
                .setStatusCode(1)
                .setMessage("SUCCEEDED")
                .setBody(anyMessage)
                .build();
        arthasStreamObserver.onNext(responseBody);
    }
}
