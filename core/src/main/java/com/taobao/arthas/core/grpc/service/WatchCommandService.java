package com.taobao.arthas.core.grpc.service;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.AutoGrpc.ResponseBody;
import com.taobao.arthas.core.AutoGrpc.StringValue;
import com.taobao.arthas.core.AutoGrpc.WatchGrpc;
import com.taobao.arthas.core.AutoGrpc.WatchRequest;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.advisor.InvokeTraceable;
import com.taobao.arthas.core.command.model.EnhancerModel;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.monitor200.AbstractTraceAdviceListener;
import com.taobao.arthas.core.grpc.model.WatchRequestModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.observer.impl.ArthasStreamObserverImpl;
import com.taobao.arthas.core.grpc.service.advisor.WatchRpcAdviceListener;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.view.Ansi;
import io.grpc.stub.StreamObserver;

import java.lang.instrument.Instrumentation;


public class WatchCommandService extends WatchGrpc.WatchImplBase {

    private static final Logger logger = LoggerFactory.getLogger(WatchCommandService.class);

    private WatchRequestModel watchRequestModel;

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
        watchRequestModel = new WatchRequestModel(watchRequest);
        ArthasStreamObserverImpl<ResponseBody> newArthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver, watchRequestModel, sessionManager, grpcJobController);
        // arthasStreamObserver 传入到advisor中，实现异步传输数据
        if(grpcJobController.containsJob(watchRequestModel.getJobId())){
            arthasStreamObserver = grpcJobController.getGrpcJob(watchRequest.getJobId());
            if(arthasStreamObserver != null && arthasStreamObserver.getPorcessStatus() == ExecStatus.RUNNING){
                WatchRpcAdviceListener listener = (WatchRpcAdviceListener) AdviceWeaver.listener(arthasStreamObserver.getListener().id());
                watchRequestModel.setListenerId(listener.id());
                arthasStreamObserver.setRequestModel(watchRequestModel);
                listener.setArthasStreamObserver(arthasStreamObserver);
                arthasStreamObserver.appendResult(new MessageModel("SUCCESS CHANGE!!!!!!!!!!!"));
                newArthasStreamObserver.end(0,"修改成功!!!");
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
                enhance(arthasStreamObserver);
            } catch (Throwable t) {
                logger.error("Error during processing the command:", t);
                arthasStreamObserver.end(-1, "Error during processing the command: " + t.getClass().getName() + ", message:" + t.getMessage()
                        + ", please check $HOME/logs/arthas/arthas.log for more details." );
            }
        }
    }



    AdviceListener getAdviceListenerWithId(WatchRequestModel watchRequestModel, ArthasStreamObserver arthasStreamObserver) {
        if (watchRequestModel.getListenerId()!= 0) {
            AdviceListener listener = AdviceWeaver.listener(watchRequestModel.getListenerId());
            if (listener != null) {
                return listener;
            }
        }
        return new WatchRpcAdviceListener(arthasStreamObserver, GlobalOptions.verbose || watchRequestModel.isVerbose());
    }

    void enhance(ArthasStreamObserver arthasStreamObserver) {
        Session session = arthasStreamObserver.session();

        if (!session.tryLock()) {
            String msg = "someone else is enhancing classes, pls. wait.";
//            arthasStreamObserver.appendResult(new EnhancerModel(null, false, msg));
            arthasStreamObserver.end(-1, msg);
            return;
        }
        EnhancerAffect effect = null;
        int lock = session.getLock();
        try {
            Instrumentation inst = session.getInstrumentation();
            AdviceListener listener = getAdviceListenerWithId(watchRequestModel, arthasStreamObserver);
            if (listener == null) {
                logger.error("advice listener is null");
                String msg = "advice listener is null, check arthas log";
//                arthasStreamObserver.appendResult(new EnhancerModel(effect, false, msg));
                arthasStreamObserver.end(-1, msg);
                return;
            }
            boolean skipJDKTrace = false;
            if(listener instanceof AbstractTraceAdviceListener) {
                skipJDKTrace = ((AbstractTraceAdviceListener) listener).getCommand().isSkipJDKTrace();
            }

            Enhancer enhancer = new Enhancer(listener, listener instanceof InvokeTraceable, skipJDKTrace, watchRequestModel.getClassNameMatcher(), watchRequestModel.getClassNameExcludeMatcher(), watchRequestModel.getMethodNameMatcher());
            // 注册通知监听器
            arthasStreamObserver.register(listener, enhancer);
            effect = enhancer.enhance(inst, watchRequestModel.getMaxNumOfMatchedClass());
            if (effect.getThrowable() != null) {
                String msg = "error happens when enhancing class: "+effect.getThrowable().getMessage();
//                arthasStreamObserver.appendResult(new EnhancerModel(effect, false, msg));
                arthasStreamObserver.end(-1, msg + ", check arthas log: " + LogUtil.loggingFile());
                return;
            }

            if (effect.cCnt() == 0 || effect.mCnt() == 0) {
                // no class effected
                if (!StringUtils.isEmpty(effect.getOverLimitMsg())) {
                    String msg = "no class effected";
//                    arthasStreamObserver.appendResult(new EnhancerModel(effect, false));
                    arthasStreamObserver.end(-1, msg);
                    return;
                }
                // might be method code too large
//                arthasStreamObserver.appendResult(new EnhancerModel(effect, false, "No class or method is affected"));

                String smCommand = Ansi.ansi().fg(Ansi.Color.GREEN).a("sm CLASS_NAME METHOD_NAME").reset().toString();
                String optionsCommand = Ansi.ansi().fg(Ansi.Color.GREEN).a("options unsafe true").reset().toString();
                String javaPackage = Ansi.ansi().fg(Ansi.Color.GREEN).a("java.*").reset().toString();
                String resetCommand = Ansi.ansi().fg(Ansi.Color.GREEN).a("reset CLASS_NAME").reset().toString();
                String logStr = Ansi.ansi().fg(Ansi.Color.GREEN).a(LogUtil.loggingFile()).reset().toString();
                String issueStr = Ansi.ansi().fg(Ansi.Color.GREEN).a("https://github.com/alibaba/arthas/issues/47").reset().toString();
                String msg = "No class or method is affected, try:\n"
                        + "1. Execute `" + smCommand + "` to make sure the method you are tracing actually exists (it might be in your parent class).\n"
                        + "2. Execute `" + optionsCommand + "`, if you want to enhance the classes under the `" + javaPackage + "` package.\n"
                        + "3. Execute `" + resetCommand + "` and try again, your method body might be too large.\n"
                        + "4. Match the constructor, use `<init>`, for example: `watch demo.MathGame <init>`\n"
                        + "5. Check arthas log: " + logStr + "\n"
                        + "6. Visit " + issueStr + " for more details.";
                arthasStreamObserver.end(-1, msg);
                return;
            }
            arthasStreamObserver.appendResult(new EnhancerModel(effect, true));

            //异步执行，在RpcAdviceListener中结束
        } catch (Throwable e) {
            String msg = "error happens when enhancing class: "+e.getMessage();
            logger.error(msg, e);
//            arthasStreamObserver.appendResult(new EnhancerModel(effect, false, msg));
            arthasStreamObserver.end(-1, msg);
        } finally {
            if (session.getLock() == lock) {
                // enhance结束后解锁
                arthasStreamObserver.session().unLock();
            }
        }
    }

}
