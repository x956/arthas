package com.taobao.arthas.core.grpc.view;

import com.taobao.arthas.core.command.model.StatusModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;

/**
 * @author xuyang 2023/8/15
 */
public class GrpcStatusView extends GrpcResultView<StatusModel> {

    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, StatusModel result) {
        if (result.getMessage() != null) {
            arthasStreamObserver.write(result.getMessage());
        }
    }
}
