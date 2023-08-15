package com.taobao.arthas.core.grpc.service;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;

public class GrpcCommandTask implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(GrpcCommandTask.class);

    private ArthasStreamObserver arthasStreamObserver;

    private WatchCommandService watchCommandService;

    public GrpcCommandTask(ArthasStreamObserver arthasStreamObserver, WatchCommandService watchCommandService) {
        this.arthasStreamObserver = arthasStreamObserver;
        this.watchCommandService = watchCommandService;
    }

    @Override
    public void run() {
        try {
            this.watchCommandService.enhance(arthasStreamObserver);
        } catch (Throwable t) {
            logger.error("Error during processing the command:", t);
            arthasStreamObserver.end(1, "Error during processing the command: " + t.getClass().getName() + ", message:" + t.getMessage()
                    + ", please check $HOME/logs/arthas/arthas.log for more details." );
        }
    }
}
