package com.taobao.arthas.core.grpc.service.advisor;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AccessPoint;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.WatchModel;
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

    private WatchCommandService watchCommandService;

    private ArthasStreamObserver arthasStreamObserver;

    public WatchRpcAdviceListener(WatchCommandService watchCommandService, ArthasStreamObserver arthasStreamObserver, boolean verbose) {
        this.watchCommandService = watchCommandService;
        this.arthasStreamObserver = arthasStreamObserver;
        super.setVerbose(verbose);
    }

    private boolean isFinish() {
        return watchCommandService.isFinish() || !watchCommandService.isBefore() && !watchCommandService.isException() && !watchCommandService.isSuccess();
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
        if (watchCommandService.isBefore()) {
            watching(Advice.newForBefore(loader, clazz, method, target, args));
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        Advice advice = Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject);
        if (watchCommandService.isSuccess()) {
            watching(advice);
        }
        finishing(advice);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        if (watchCommandService.isException()) {
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
            System.out.println("rpc watch advice开始正式执行");
            double cost = threadLocalWatch.costInMillis();
            boolean conditionResult = isConditionMet(watchCommandService.getConditionExpress(), advice, cost);
            if (this.isVerbose()) {
                arthasStreamObserver.write("Condition express: " + watchCommandService.getConditionExpress() + " , result: " + conditionResult + "\n");
            }
            if (conditionResult) {
                Object value = getExpressionResult(watchCommandService.getExpress(), advice, cost);

                WatchModel model = new WatchModel();
                model.setTs(new Date());
                model.setCost(cost);
                model.setValue(new ObjectVO(value, watchCommandService.getExpand()));
                model.setSizeLimit(watchCommandService.getSizeLimit());
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
                if (isLimitExceeded(watchCommandService.getNumberOfLimit(), arthasStreamObserver.times().get())) {
                    abortProcess(arthasStreamObserver, watchCommandService.getNumberOfLimit());
                }
            }
        } catch (Throwable e) {
            logger.warn("watch failed.", e);
            arthasStreamObserver.end(-1, "watch failed, condition is: " + watchCommandService.getConditionExpress() + ", express is: "
                    + watchCommandService.getExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
        }
    }
}
