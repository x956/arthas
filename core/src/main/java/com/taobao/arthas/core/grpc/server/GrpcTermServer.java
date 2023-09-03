package com.taobao.arthas.core.grpc.server;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.grpc.service.GrpcJobController;
import com.taobao.arthas.core.grpc.service.PwdCommandService;
import com.taobao.arthas.core.grpc.service.SystemPropertyCommandService;
import com.taobao.arthas.core.grpc.service.WatchCommandService;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.SessionManager;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class GrpcTermServer extends TermServer {

    private static final Logger logger = LoggerFactory.getLogger(GrpcTermServer.class);
    private int port;
    private Server grpcServer;
    private final SessionManager sessionManager;


    public GrpcTermServer(int port, SessionManager sessionManager) {
        this.port = port;
        this.sessionManager = sessionManager;
    }

    @Override
    public TermServer termHandler(Handler<Term> handler) {
        return this;
    }

    @Override
    public TermServer listen(Handler<Future<TermServer>> listenHandler) {
        try {
            GrpcJobController grpcJobController = new GrpcJobController();
            grpcServer = ServerBuilder.forPort(port)
                    .addService(new PwdCommandService(sessionManager,grpcJobController))
                    .addService(new SystemPropertyCommandService(sessionManager,grpcJobController))
                    .addService(new WatchCommandService(sessionManager,grpcJobController))
                    .build()
                    .start();
            logger.info("Server started, listening on " + port);
            Runtime.getRuntime().addShutdownHook(new Thread("grpc-server-shutdown") {
                @Override
                public void run() {
                if (grpcServer != null) {
                    grpcServer.shutdown();
                }
                }
            });
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public int actualPort() {
        return grpcServer.getPort();
    }

    @Override
    public void close() {
        close(null);
    }

    @Override
    public void close(Handler<Future<Void>> completionHandler) {
        if (grpcServer != null) {
            grpcServer.shutdown();
            if (completionHandler != null) {
                completionHandler.handle(Future.<Void>succeededFuture());
            }
        } else {
            if (completionHandler != null) {
                completionHandler.handle(Future.<Void>failedFuture("grpc term server not started"));
            }
        }
    }
}
