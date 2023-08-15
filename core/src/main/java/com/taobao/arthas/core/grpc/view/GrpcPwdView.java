package com.taobao.arthas.core.grpc.view;

import com.taobao.arthas.core.command.model.PwdModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;

/**
 * @author xuyang 2023/8/15
 */
public class GrpcPwdView extends GrpcResultView<PwdModel> {


    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, PwdModel result) {
        arthasStreamObserver.write(result.getWorkingDir());
    }
}
