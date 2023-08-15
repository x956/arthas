package com.taobao.arthas.core.grpc.view;

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
        arthasStreamObserver.write("method=" + model.getClassName() + "." + model.getMethodName() + " location=" + model.getAccessPoint() + "\n");
        arthasStreamObserver.write("ts=" + DateUtils.formatDate(model.getTs()) + "; [cost=" + model.getCost() + "ms] result=" + result + "\n");
    }
}
