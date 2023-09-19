package com.taobao.arthas.core.grpc.server;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

public class GrpcWebProxyServer extends TermServer {

    private static final Logger logger = LoggerFactory.getLogger(GrpcWebProxyServer.class);

    private int grpcWebProxyPort;
    private int grpcPort;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Channel channel;

    private ServerBootstrap serverBootstrap;

    public GrpcWebProxyServer(int grpcPort, int grpcWebProxyPort) {
        this.grpcPort = grpcPort;
        this.grpcWebProxyPort = grpcWebProxyPort;
        // Configure the grpc web proxy server.
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
    }

    @Override
    public TermServer termHandler(Handler<Term> handler) {
        return null;
    }

    @Override
    public TermServer listen(Handler<Future<TermServer>> listenHandler) {
        Thread grpcProxyServer= new Thread("grpc-web-proxy-start"){
            @Override
            public void run() {
                try {
                    serverBootstrap = new ServerBootstrap();
                    serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                            .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new GrpcWebProxyServerInitializer(grpcPort));
                    Channel channel = serverBootstrap.bind(grpcWebProxyPort).sync().channel();
                    logger.info("grpc web proxy server started, listening on" + grpcWebProxyPort);
                    channel.closeFuture().sync();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        grpcProxyServer.start();
        return this;
    }

    @Override
    public int actualPort() {
        int boundPort = ((InetSocketAddress) channel.localAddress()).getPort();
        return boundPort;
    }

    @Override
    public void close() {
        close(null);
    }

    @Override
    public void close(Handler<Future<Void>> completionHandler) {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }else if(workerGroup != null){
            workerGroup.shutdownGracefully();
            if (completionHandler != null) {
                completionHandler.handle(Future.<Void>succeededFuture());
            }
        }else {
            if (completionHandler != null) {
                completionHandler.handle(Future.<Void>failedFuture("grpc web proxy term server not started"));
            }
        }
    }
}
