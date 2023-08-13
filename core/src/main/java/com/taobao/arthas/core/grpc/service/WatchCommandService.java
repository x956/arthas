package com.taobao.arthas.core.grpc.service;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.AutoGrpc.WatchGrpc;
import com.taobao.arthas.core.AutoGrpc.WatchRequest;
import com.taobao.arthas.core.AutoGrpc.WatchResponse;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.advisor.InvokeTraceable;
import com.taobao.arthas.core.command.monitor200.AbstractTraceAdviceListener;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.observer.impl.ArthasStreamObserverImpl;
import com.taobao.arthas.core.grpc.service.advisor.WatchRpcAdviceListener;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.Ansi;
import io.grpc.stub.StreamObserver;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;


public class WatchCommandService extends WatchGrpc.WatchImplBase {

    private static final Logger logger = LoggerFactory.getLogger(WatchCommandService.class);

    private WatchRequest watchRequest;
    private String classPattern;
    private String methodPattern;
    private String express;
    private String conditionExpress;
    private boolean isBefore = false;
    private boolean isFinish = false;
    private boolean isException = false;
    private boolean isSuccess = false;
    private Integer expand = 1;
    private Integer sizeLimit = 10 * 1024 * 1024;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;

    protected static final List<String> EMPTY = Collections.emptyList();
    public static final String[] EXPRESS_EXAMPLES = { "params", "returnObj", "throwExp", "target", "clazz", "method",
            "{params,returnObj}", "params[0]" };
    private String excludeClassPattern;

    private Matcher classNameMatcher;
    private Matcher classNameExcludeMatcher;
    private Matcher methodNameMatcher;

    private long listenerId;

    private boolean verbose;

    private int maxNumOfMatchedClass;

    private Instrumentation instrumentation;

    public WatchCommandService(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    @Override
    public String toString() {
        return "WatchCommandService{" +
                "classPattern='" + classPattern + '\'' +
                ", methodPattern='" + methodPattern + '\'' +
                ", express='" + express + '\'' +
                ", conditionExpress='" + conditionExpress + '\'' +
                ", isBefore=" + isBefore +
                ", isFinish=" + isFinish +
                ", isException=" + isException +
                ", isSuccess=" + isSuccess +
                ", expand=" + expand +
                ", sizeLimit=" + sizeLimit +
                ", isRegEx=" + isRegEx +
                ", numberOfLimit=" + numberOfLimit +
                ", excludeClassPattern='" + excludeClassPattern + '\'' +
                ", listenerId=" + listenerId +
                ", verbose=" + verbose +
                ", maxNumOfMatchedClass=" + maxNumOfMatchedClass +
                '}';
    }

    @Override
    public void watch(WatchRequest watchRequest, StreamObserver<WatchResponse> responseObserver){
        // 解析watchRequest 参数
        // 需要参照EnhancerCommand.process写
        parseRequestParams(watchRequest);
        System.out.println(this.toString());

        System.out.println("参数初始化完成");
        // arthasStreamObserver 传入到advisor中，实现异步传输数据
        ArthasStreamObserver<WatchResponse> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver);
//        enhance(arthasStreamObserver);
        GrpcCommandTask grpcCommandTask = new GrpcCommandTask(arthasStreamObserver, this);
        System.out.println("开始execute...");
        ArthasBootstrap.getInstance().execute(grpcCommandTask);
        System.out.println("enhance 激活成功,开始运行...");

//        arthasStreamObserver.onCompleted();
//        System.out.println("结束了,如果客户端啥也没有输出,说明服务端根本没有生产数据...");
//        WatchResponse watchResponse = WatchResponse.newBuilder().build();
//        arthasStreamObserver.onNext(watchResponse);
//        arthasStreamObserver.onCompleted();
    }



    private Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
        }
        return classNameMatcher;
    }

    public Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
        }
        return methodNameMatcher;
    }
    public Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    public void parseRequestParams(WatchRequest watchRequest){
        this.watchRequest = watchRequest;
        this.classPattern = watchRequest.getClassPattern();
        this.methodPattern = watchRequest.getMethodPattern();
        this.express = watchRequest.getExpress();
        this.conditionExpress = watchRequest.getConditionExpress();
        this.isBefore = watchRequest.getIsBefore();
        this.isFinish = watchRequest.getIsFinish();
        this.isException = watchRequest.getIsException();
        this.isSuccess = watchRequest.getIsSuccess();
        if (!watchRequest.getIsBefore() && !watchRequest.getIsFinish() && !watchRequest.getIsException() && !watchRequest.getIsSuccess()) {
            this.isFinish = true;
        }
        if (watchRequest.getExpand() == 0) {
            this.expand = 1;
        } else {
            this.expand = watchRequest.getExpand();
        }
        if (watchRequest.getSizeLimit() == 0) {
            this.sizeLimit = 10 * 1024 * 1024;
        } else {
            this.sizeLimit = watchRequest.getSizeLimit();
        }
        this.isRegEx = watchRequest.getIsRegEx();
        if (watchRequest.getNumberOfLimit() == 0) {
            this.numberOfLimit = 100;
        } else {
            this.numberOfLimit = watchRequest.getNumberOfLimit();
        }
        if(watchRequest.getExcludeClassPattern().equals("")){
            this.excludeClassPattern = null;
        }else {
            this.excludeClassPattern = watchRequest.getExcludeClassPattern();
        }
        this.listenerId = watchRequest.getListenerId();
        this.verbose = watchRequest.getVerbose();
        if(watchRequest.getMaxNumOfMatchedClass() == 0){
            this.maxNumOfMatchedClass = 50;
        }else {
            this.maxNumOfMatchedClass = watchRequest.getMaxNumOfMatchedClass();
        }
    }


    AdviceListener getAdviceListenerWithId(WatchRequest watchRequest, ArthasStreamObserver arthasStreamObserver) {
        if (watchRequest.getListenerId()!= 0) {
            AdviceListener listener = AdviceWeaver.listener(watchRequest.getListenerId());
            if (listener != null) {
                return listener;
            }
        }
        return new WatchRpcAdviceListener(this, arthasStreamObserver, GlobalOptions.verbose || watchRequest.getVerbose());
    }

    void enhance(ArthasStreamObserver arthasStreamObserver) {
        // 此函数参照EnhancerCommand构建
//         TOD 找到session的赋值位置
//         TOD 找到Instrumentation在哪赋值/如何在这里获取ArthasBoostrap中的变量
//         TOD 明确这个process的作用
//        Session session = process.session();
//        if (!session.tryLock()) {
//            String msg = "someone else is enhancing classes, pls. wait.";
//            process.appendResult(new EnhancerModel(null, false, msg));
//            process.end(-1, msg);
//            return;
//        }
        EnhancerAffect effect = null;
//        int lock = session.getLock();
        try {
            // 该参数来自启动服务器时，ShellServerOptions  # com.taobao.arthas.core.server.ArthasBootstrap 394行
            Instrumentation inst = this.instrumentation;

            // 获取监听器
            AdviceListener listener = getAdviceListenerWithId(watchRequest, arthasStreamObserver);
            if (listener == null) {
                logger.error("advice listener is null");
                String msg = "advice listener is null, check arthas log";
                WatchResponse watchResponse = WatchResponse.newBuilder().clear().setMessage(msg).build();
                arthasStreamObserver.onNext(watchResponse);
                arthasStreamObserver.onCompleted();
                return;
//                process.appendResult(new EnhancerModel(effect, false, msg));
//                process.end(-1, msg);
//                return;
            }
            boolean skipJDKTrace = false;
            if(listener instanceof AbstractTraceAdviceListener) {
                skipJDKTrace = ((AbstractTraceAdviceListener) listener).getCommand().isSkipJDKTrace();
            }

            Enhancer enhancer = new Enhancer(listener, listener instanceof InvokeTraceable, skipJDKTrace, getClassNameMatcher(), getClassNameExcludeMatcher(), getMethodNameMatcher());
            // 注册通知监听器
            arthasStreamObserver.register(listener, enhancer);
            effect = enhancer.enhance(inst, this.maxNumOfMatchedClass);
            System.out.println("注册完成？？？");

            if (effect.getThrowable() != null) {
                String msg = "error happens when enhancing class: "+effect.getThrowable().getMessage();
                WatchResponse watchResponse = WatchResponse.newBuilder().clear().setMessage(msg).build();
                arthasStreamObserver.onNext(watchResponse);
                arthasStreamObserver.onCompleted();
//                process.appendResult(new EnhancerModel(effect, false, msg));
//                process.end(1, msg + ", check arthas log: " + LogUtil.loggingFile());
                return;
            }

            if (effect.cCnt() == 0 || effect.mCnt() == 0) {
                // no class effected
                if (!StringUtils.isEmpty(effect.getOverLimitMsg())) {
                    String msg = "effect.getOverLimitMsg()不为空"  + effect.getOverLimitMsg();
                    WatchResponse watchResponse = WatchResponse.newBuilder().clear().setMessage(msg).build();
                    arthasStreamObserver.onNext(watchResponse);
                    arthasStreamObserver.onCompleted();
//                    process.appendResult(new EnhancerModel(effect, false));
//                    process.end(-1);
                    return;
                }
                // might be method code too large
//                process.appendResult(new EnhancerModel(effect, false, "No class or method is affected"));
                String msg1 = "No class or method is affected";
                WatchResponse watchResponse = WatchResponse.newBuilder().clear().setMessage(msg1).build();
                arthasStreamObserver.onNext(watchResponse);

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
//                process.end(-1, msg);
                WatchResponse watchResponse2 = WatchResponse.newBuilder().clear().setMessage(msg).build();
                arthasStreamObserver.onNext(watchResponse2);
                arthasStreamObserver.onCompleted();
                return;
            }

//            // 这里做个补偿,如果在enhance期间,unLock被调用了,则补偿性放弃
//            if (session.getLock() == lock) {
//                if (process.isForeground()) {
//                    process.echoTips(Constants.Q_OR_CTRL_C_ABORT_MSG + "\n");
//                }
//            }
//
//            process.appendResult(new EnhancerModel(effect, true));

            //异步执行，在AdviceListener中结束
        } catch (Throwable e) {
            String msg = "error happens when enhancing class: "+e.getMessage();
            logger.error(msg, e);
            WatchResponse watchResponse = WatchResponse.newBuilder().clear().setMessage(msg).build();
            arthasStreamObserver.onNext(watchResponse);
            arthasStreamObserver.onCompleted();
//            process.appendResult(new EnhancerModel(effect, false, msg));
//            process.end(-1, msg);
        } finally {
            System.out.println("执行结束!!!");
//            if (session.getLock() == lock) {
//                // enhance结束后解锁
//                process.session().unLock();
//            }
        }
    }


    public String getClassPattern() {
        return classPattern;
    }

    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    public String getExpress() {
        return express;
    }

    public void setExpress(String express) {
        this.express = express;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    public boolean isBefore() {
        return isBefore;
    }

    public void setBefore(boolean before) {
        isBefore = before;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public void setFinish(boolean finish) {
        isFinish = finish;
    }

    public boolean isException() {
        return isException;
    }

    public void setException(boolean exception) {
        isException = exception;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public Integer getExpand() {
        return expand;
    }

    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    public String getExcludeClassPattern() {
        return excludeClassPattern;
    }

    public void setExcludeClassPattern(String excludeClassPattern) {
        this.excludeClassPattern = excludeClassPattern;
    }

    public void setClassNameMatcher(Matcher classNameMatcher) {
        this.classNameMatcher = classNameMatcher;
    }

    public void setClassNameExcludeMatcher(Matcher classNameExcludeMatcher) {
        this.classNameExcludeMatcher = classNameExcludeMatcher;
    }

    public void setMethodNameMatcher(Matcher methodNameMatcher) {
        this.methodNameMatcher = methodNameMatcher;
    }

    public long getListenerId() {
        return listenerId;
    }

    public void setListenerId(long listenerId) {
        this.listenerId = listenerId;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public int getMaxNumOfMatchedClass() {
        return maxNumOfMatchedClass;
    }

    public void setMaxNumOfMatchedClass(int maxNumOfMatchedClass) {
        this.maxNumOfMatchedClass = maxNumOfMatchedClass;
    }
}
