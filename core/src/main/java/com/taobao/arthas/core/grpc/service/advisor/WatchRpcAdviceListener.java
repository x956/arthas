package com.taobao.arthas.core.grpc.service.advisor;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.google.protobuf.Timestamp;
import com.taobao.arthas.core.AutoGrpc.WatchResponse;
import com.taobao.arthas.core.advisor.AccessPoint;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.service.WatchCommandService;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.time.Instant;

/**
 * @author beiwei30 on 29/11/2016.
 */
public class WatchRpcAdviceListener extends RpcAdviceListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WatchRpcAdviceListener.class);
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
//    private WatchCommand command;

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
                String message = "Condition express: " + watchCommandService.getConditionExpress() + " , result: " + conditionResult + "\n";
                WatchResponse watchResponse = WatchResponse.newBuilder().clear().setMessage(message).build();
                arthasStreamObserver.onNext(watchResponse);
//                process.write("Condition express: " + command.getConditionExpress() + " , result: " + conditionResult + "\n");
            }
            if (conditionResult) {
                Object value = getExpressionResult(watchCommandService.getExpress(), advice, cost);
                WatchResponse.Builder watchResponseBuilder = WatchResponse.newBuilder();
                // 创建一个表示当前日期和时间的 Instant 对象
                Instant now = Instant.now();
                // 转换为 google.protobuf.Timestamp 类型
                Timestamp timestamp = Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build();
                watchResponseBuilder.setTs(timestamp)
                        .setCost(cost)
                        .setSizeLimit(watchCommandService.getSizeLimit())
                        .setClassName(advice.getClazz().getName())
                        .setMethodName(advice.getMethod().getName());
                // TODO Object类型传输
                if (advice.isBefore()) {
                    watchResponseBuilder.setAccessPoint(AccessPoint.ACCESS_BEFORE.getKey());
                } else if (advice.isAfterReturning()) {
                    watchResponseBuilder.setAccessPoint(AccessPoint.ACCESS_AFTER_RETUNING.getKey());
                } else if (advice.isAfterThrowing()) {
                    watchResponseBuilder.setAccessPoint(AccessPoint.ACCESS_AFTER_THROWING.getKey());
                }
                WatchResponse watchResponse = watchResponseBuilder.build();
                System.out.println("发送数据到客户端？");

                arthasStreamObserver.onNext(watchResponse);
                arthasStreamObserver.times().incrementAndGet();
                if (isLimitExceeded(watchCommandService.getNumberOfLimit(), arthasStreamObserver.times().get())) {
                    abortProcess(arthasStreamObserver, watchCommandService.getNumberOfLimit());
                }
                System.out.println("发送完成？");

//                WatchModel model = new WatchModel();
//                model.setTs(new Date());
//                model.setCost(cost);
//                model.setValue(new ObjectVO(value, command.getExpand()));
//                model.setSizeLimit(command.getSizeLimit());
//                model.setClassName(advice.getClazz().getName());
//                model.setMethodName(advice.getMethod().getName());
//                if (advice.isBefore()) {
//                    model.setAccessPoint(AccessPoint.ACCESS_BEFORE.getKey());
//                } else if (advice.isAfterReturning()) {
//                    model.setAccessPoint(AccessPoint.ACCESS_AFTER_RETUNING.getKey());
//                } else if (advice.isAfterThrowing()) {
//                    model.setAccessPoint(AccessPoint.ACCESS_AFTER_THROWING.getKey());
//                }
//                // 使用watchResponseBuilder构建
//
////                arthasStreamObserver.onNext(model);
//                process.appendResult(model);
//                process.times().incrementAndGet();
//                if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
//                    abortProcess(process, command.getNumberOfLimit());
//                }
            }
        } catch (Throwable e) {
            logger.warn("watch failed.", e);
            String message = "watch failed, condition is: " + watchCommandService.getConditionExpress() + ", express is: "
                    + watchCommandService.getExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.";
            WatchResponse watchResponse = WatchResponse.newBuilder().clear().setMessage(message).build();
            arthasStreamObserver.onNext(watchResponse);
            arthasStreamObserver.onError(e);
//            process.end(-1, "watch failed, condition is: " + command.getConditionExpress() + ", express is: "
//                    + command.getExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
//                    + " for more details.");
        }
    }
}
