package com.taobao.arthas.core.grpc.service.advisor;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AccessPoint;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.grpc.model.WatchRequestModel;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.service.WatchCommandService;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.util.Date;

/**
 * @author beiwei30 on 29/11/2016.
 */
public class WatchRpcAdviceListener extends RpcAdviceListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WatchRpcAdviceListener.class);
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();

    private WatchRequestModel watchRequestModel;

    private ArthasStreamObserver arthasStreamObserver;

    public WatchRpcAdviceListener(ArthasStreamObserver arthasStreamObserver, boolean verbose) {
        this.arthasStreamObserver = arthasStreamObserver;
        this.watchRequestModel = (WatchRequestModel) arthasStreamObserver.getRequestModel();
        super.setVerbose(verbose);
    }

    private boolean isFinish() {
        return watchRequestModel.isFinish() || !watchRequestModel.isBefore() && !watchRequestModel.isException() && !watchRequestModel.isSuccess();
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
        if (watchRequestModel.isBefore()) {
            watching(Advice.newForBefore(loader, clazz, method, target, args));
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        Advice advice = Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject);
        if (watchRequestModel.isSuccess()) {
            watching(advice);
        }
        finishing(advice);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        if (watchRequestModel.isException()) {
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
            System.out.println("************rpc watch advice开始正式执行,执行信息如下*****************");
            double cost = threadLocalWatch.costInMillis();
            boolean conditionResult = isConditionMet(watchRequestModel.getConditionExpress(), advice, cost);
            if (this.isVerbose()) {
                arthasStreamObserver.write("Condition express: " + watchRequestModel.getConditionExpress() + " , result: " + conditionResult + "\n");
            }
            if (conditionResult) {
                Object value = getExpressionResult(watchRequestModel.getExpress(), advice, cost);

                WatchModel model = new WatchModel();
                model.setTs(new Date());
                model.setCost(cost);
                model.setValue(new ObjectVO(value, watchRequestModel.getExpand()));
                model.setSizeLimit(watchRequestModel.getSizeLimit());
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
                if (isLimitExceeded(watchRequestModel.getNumberOfLimit(), arthasStreamObserver.times().get())) {
                    abortProcess(arthasStreamObserver, watchRequestModel.getNumberOfLimit());
                }
            }
        } catch (Throwable e) {
            logger.warn("watch failed.", e);
            arthasStreamObserver.end(-1, "watch failed, condition is: " + watchRequestModel.getConditionExpress() + ", express is: "
                    + watchRequestModel.getExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
        }
    }
}
