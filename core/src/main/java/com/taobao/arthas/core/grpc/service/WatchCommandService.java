package com.taobao.arthas.core.grpc.service;

import arthas.grpc.api.WatchGrpc;
import arthas.grpc.api.ArthasService.*;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.grpc.model.WatchCommandModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.observer.impl.ArthasStreamObserverImpl;
import com.taobao.arthas.core.grpc.service.advisor.WatchRpcAdviceListener;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.ExecStatus;
import io.grpc.stub.StreamObserver;


public class WatchCommandService extends WatchGrpc.WatchImplBase {

    private static final Logger logger = LoggerFactory.getLogger(WatchCommandService.class);

    private WatchCommandModel watchCommandModel;

    private ArthasStreamObserver arthasStreamObserver;

    private SessionManager sessionManager;

    private GrpcJobController grpcJobController;

    public WatchCommandService(SessionManager sessionManager, GrpcJobController grpcJobController) {
        this.sessionManager = sessionManager;
        this.grpcJobController = grpcJobController;
    }

    @Override
    public void watch(WatchRequest watchRequest, StreamObserver<ResponseBody> responseObserver){
        // 解析watchRequest 参数
        watchCommandModel = new WatchCommandModel(watchRequest);
        ArthasStreamObserverImpl<ResponseBody> newArthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver, watchCommandModel, sessionManager, grpcJobController);
        // arthasStreamObserver 传入到advisor中，实现异步传输数据
        if(grpcJobController.containsJob(watchCommandModel.getJobId())){
            arthasStreamObserver = grpcJobController.getGrpcJob(watchRequest.getJobId());
            if(arthasStreamObserver != null && arthasStreamObserver.getPorcessStatus() == ExecStatus.RUNNING){
                WatchRpcAdviceListener listener = (WatchRpcAdviceListener) AdviceWeaver.listener(arthasStreamObserver.getListener().id());
                watchCommandModel.setListenerId(listener.id());
                arthasStreamObserver.setRequestModel(watchCommandModel);
                listener.setArthasStreamObserver(arthasStreamObserver);
                arthasStreamObserver.appendResult(new MessageModel("SUCCESS CHANGE!!!!!!!!!!!"));
                newArthasStreamObserver.end(0,"修改成功!!!");
                return;
            }else {
                arthasStreamObserver = newArthasStreamObserver;
            }
        }else {
            arthasStreamObserver = newArthasStreamObserver;
        }
        // 创建watch任务
        WatchTask watchTask = new WatchTask();
        // 执行watch任务
        ArthasBootstrap.getInstance().execute(watchTask);
    }

    private class WatchTask implements Runnable{
        @Override
        public void run() {
            try {
                watchCommandModel.enhance(arthasStreamObserver);
            } catch (Throwable t) {
                logger.error("Error during processing the command:", t);
                arthasStreamObserver.end(-1, "Error during processing the command: " + t.getClass().getName() + ", message:" + t.getMessage()
                        + ", please check $HOME/logs/arthas/arthas.log for more details." );
            }
        }
    }
}
