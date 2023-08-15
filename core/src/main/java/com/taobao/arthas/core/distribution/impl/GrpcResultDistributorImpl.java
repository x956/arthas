package com.taobao.arthas.core.distribution.impl;

import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.view.ResultView;
import com.taobao.arthas.core.command.view.ResultViewResolver;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.core.grpc.view.GrpcResultView;
import com.taobao.arthas.core.grpc.view.GrpcResultViewResolver;

public class GrpcResultDistributorImpl implements ResultDistributor {

    private final ArthasStreamObserver arthasStreamObserver;

    private final GrpcResultViewResolver grpcResultViewResolver;

    public GrpcResultDistributorImpl(ArthasStreamObserver arthasStreamObserver, GrpcResultViewResolver resultViewResolver) {
        this.arthasStreamObserver = arthasStreamObserver;
        this.grpcResultViewResolver = resultViewResolver;
    }

    @Override
    public void appendResult(ResultModel model) {
        GrpcResultView resultView = grpcResultViewResolver.getResultView(model);
        if (resultView != null) {
            resultView.draw(arthasStreamObserver, model);
        }
    }

    @Override
    public void close() {

    }
}
