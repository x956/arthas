package com.taobao.arthas.core.grpc.view;

import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.view.ResetView;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Command result view for grpc client.
 * Note: Result view is a reusable and stateless instance
 *
 * @author xuyang 2023/8/15
 */
public abstract class GrpcResultView<T extends ResultModel> {

    /**
     * formatted printing data to grpc client
     *
     * @param arthasStreamObserver
     */
    public abstract void draw(ArthasStreamObserver arthasStreamObserver, T result);

    /**
     * write str and append a new line
     *
     * @param arthasStreamObserver
     * @param str
     */
    protected void writeln(ArthasStreamObserver arthasStreamObserver, String str) {
        arthasStreamObserver.write(str + "\n");
    }
}
