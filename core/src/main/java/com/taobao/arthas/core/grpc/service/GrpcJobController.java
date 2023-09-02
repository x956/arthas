package com.taobao.arthas.core.grpc.service;

import com.taobao.arthas.core.grpc.observer.ArthasStreamObserver;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GrpcJobController{

    private final static Map<Long/*JOB_ID*/, ArthasStreamObserver> jobs
            = new ConcurrentHashMap<Long, ArthasStreamObserver>();

    public Set<Long> getJobIds(){
        return jobs.keySet();
    }

    public void registerGrpcJob(long jobId,ArthasStreamObserver arthasStreamObserver){
        jobs.put(jobId, arthasStreamObserver);
    }

    public void unRegisterGrpcJob(long jobId){
        if(jobs.containsKey(jobId)){
            jobs.remove(jobId);
        }
    }
    public boolean containsJob(long jobId){
        return jobs.containsKey(jobId);
    }

    public ArthasStreamObserver getGrpcJob(long jobId){
        if(this.containsJob(jobId)){
            return jobs.get(jobId);
        }else {
            return null;
        }
    }
}
