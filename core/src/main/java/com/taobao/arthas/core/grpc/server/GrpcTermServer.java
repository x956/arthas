package com.taobao.arthas.core.grpc.server;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.grpc.ArthasService;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class GrpcTermServer extends TermServer {

    private static final Logger logger = LoggerFactory.getLogger(GrpcTermServer.class);

//    private Handler<Term> termHandler;
//    private NettyWebsocketTtyBootstrap bootstrap;
    private int port;
    private Server grpcServer;


    public GrpcTermServer(int port) {
//        this.hostIp = hostIp;
        this.port = port;
//        this.connectionTimeout = connectionTimeout;
//        this.workerGroup = workerGroup;
//        this.httpSessionManager = httpSessionManager;
    }

    @Override
    public TermServer termHandler(Handler<Term> handler) {
        return this;
    }

    @Override
    public TermServer listen(Handler<Future<TermServer>> listenHandler) {
        try {
            grpcServer = ServerBuilder.forPort(port)
                    .addService(new ArthasService())
                    .build()
                    .start();
            logger.info("Server started, listening on " + port);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                    System.err.println("*** shutting down gRPC server since JVM is shutting down");
                    if (grpcServer != null) {
                        grpcServer.shutdown();
                    }
                    System.err.println("*** server shut down");
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
