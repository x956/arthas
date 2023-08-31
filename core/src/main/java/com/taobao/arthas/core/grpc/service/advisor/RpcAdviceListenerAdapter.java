package com.taobao.arthas.core.grpc.service.advisor;

import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;

public abstract class RpcAdviceListenerAdapter extends AdviceListenerAdapter {

    /**
     * 超过次数上限，则不再输出，命令终止
     *
     * @param arthasStreamObserver the streamObserver to be aborted
     * @param limit   the limit to be printed
     */
    protected void abortProcess(ArthasStreamObserver arthasStreamObserver, int limit) {
        String msg = "Command execution times exceed limit: " + limit
                + ", so command will exit. You can set it with -n option.\n";
//        arthasStreamObserver.appendResult(new MessageModel(msg));
        arthasStreamObserver.end();
    }
}
