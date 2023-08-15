package com.taobao.arthas.core.grpc.view;

import com.taobao.arthas.core.command.model.SystemPropertyModel;
import com.taobao.arthas.core.command.view.ViewRenderUtil;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;

public class GrpcSystemPropertyView extends GrpcResultView<SystemPropertyModel>{

    @Override
    public void draw(ArthasStreamObserver arthasStreamObserver, SystemPropertyModel result) {
        arthasStreamObserver.write(ViewRenderUtil.renderKeyValueTable(result.getProps(), 80));
    }

}
