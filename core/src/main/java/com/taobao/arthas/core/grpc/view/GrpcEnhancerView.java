package com.taobao.arthas.core.grpc.view;

import arthas.grpc.api.ArthasService.ResponseBody;
import com.taobao.arthas.core.command.model.EnhancerModel;
import com.taobao.arthas.core.command.view.ViewRenderUtil;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;

/**
 * Term grpc view for EnhancerModel
 * @author xuyang 2023/8/15
 */
public class GrpcEnhancerView extends GrpcResultView<EnhancerModel> {
    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, EnhancerModel result) {
        // ignore enhance result status, judge by the following output
        if (result.getEffect() != null) {
            String msg = ViewRenderUtil.renderEnhancerAffect(result.getEffect());
            ResponseBody responseBody  = ResponseBody.newBuilder()
                    .setSessionId(arthasStreamObserver.session().getSessionId())
                    .setStatusCode(0)
                    .setMessage(msg)
                    .build();
            arthasStreamObserver.onNext(responseBody);
        }
    }
}
