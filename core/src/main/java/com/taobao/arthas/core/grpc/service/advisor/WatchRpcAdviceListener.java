package com.taobao.arthas.core.grpc.service.advisor;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AccessPoint;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.grpc.model.WatchCommandModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.util.Date;

/**
 * @author beiwei30 on 29/11/2016.
 */
public class WatchRpcAdviceListener extends RpcAdviceListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WatchRpcAdviceListener.class);
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();

    private WatchCommandModel watchCommandModel;

    private ArthasStreamObserver arthasStreamObserver;

    public WatchRpcAdviceListener(ArthasStreamObserver arthasStreamObserver, boolean verbose) {
        this.arthasStreamObserver = arthasStreamObserver;
        this.watchCommandModel = (WatchCommandModel) arthasStreamObserver.getRequestModel();
        super.setVerbose(verbose);
    }

    public void setArthasStreamObserver(ArthasStreamObserver arthasStreamObserver) {
        this.arthasStreamObserver = arthasStreamObserver;
        this.watchCommandModel = (WatchCommandModel) arthasStreamObserver.getRequestModel();
    }

    private boolean isFinish() {
        return watchCommandModel.isFinish() || !watchCommandModel.isBefore() && !watchCommandModel.isException() && !watchCommandModel.isSuccess();
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
        if (watchCommandModel.isBefore()) {
            watching(Advice.newForBefore(loader, clazz, method, target, args));
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        Advice advice = Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject);
        if (watchCommandModel.isSuccess()) {
            watching(advice);
        }
        finishing(advice);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        if (watchCommandModel.isException()) {
            watching(advice);
        }
        finishing(advice);
    }

    private void finishing(Advice advice) {
        if (isFinish()) {
            watching(advice);
        }
    }


    private void watching(Advice advice) {
        try {
            // 本次调用的耗时
            System.out.println("************job:  "+ arthasStreamObserver.getJobId() + "  rpc watch advice开始正式执行,执行信息如下*****************");
            System.out.println("listener ID: + " + arthasStreamObserver.getListener().id());
            System.out.println("参数: \n" + watchCommandModel.toString());
            System.out.println("###################***************** \n\n");
            double cost = threadLocalWatch.costInMillis();
            boolean conditionResult = isConditionMet(watchCommandModel.getConditionExpress(), advice, cost);
            if (this.isVerbose()) {
                String msg = "Condition express: " + watchCommandModel.getConditionExpress() + " , result: " + conditionResult + "\n";
                arthasStreamObserver.appendResult(new MessageModel(msg));
            }
            if (conditionResult) {
                Object value = getExpressionResult(watchCommandModel.getExpress(), advice, cost);

                WatchModel model = new WatchModel();
                model.setTs(new Date());
                model.setCost(cost);
                model.setValue(new ObjectVO(value, watchCommandModel.getExpand()));
                model.setSizeLimit(watchCommandModel.getSizeLimit());
                model.setClassName(advice.getClazz().getName());
                model.setMethodName(advice.getMethod().getName());
                if (advice.isBefore()) {
                    model.setAccessPoint(AccessPoint.ACCESS_BEFORE.getKey());
                } else if (advice.isAfterReturning()) {
                    model.setAccessPoint(AccessPoint.ACCESS_AFTER_RETUNING.getKey());
                } else if (advice.isAfterThrowing()) {
                    model.setAccessPoint(AccessPoint.ACCESS_AFTER_THROWING.getKey());
                }
                arthasStreamObserver.appendResult(model);
                arthasStreamObserver.times().incrementAndGet();
                if (isLimitExceeded(watchCommandModel.getNumberOfLimit(), arthasStreamObserver.times().get())) {
                    abortProcess(arthasStreamObserver, watchCommandModel.getNumberOfLimit());
                }
            }
        } catch (Throwable e) {
            logger.warn("watch failed.", e);
            arthasStreamObserver.end(-1, "watch failed, condition is: " + watchCommandModel.getConditionExpress() + ", express is: "
                    + watchCommandModel.getExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
        }
    }
}
