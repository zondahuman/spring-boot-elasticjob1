package com.abin.lee.elasticjob.job;

import com.abin.lee.elasticjob.dao.TaskRepository;
import com.abin.lee.elasticjob.entity.JobTask;
import com.dangdang.ddframe.job.executor.ShardingContexts;
import com.dangdang.ddframe.job.lite.api.listener.AbstractDistributeOnceElasticJobListener;

import javax.annotation.Resource;


/**
 * Created by IntelliJ IDEA.
 *
 * 实现分布式任务监听器
 * 如果任务有分片，分布式监听器会在总的任务开始前执行一次，结束时执行一次
 **/
public class ElasticJobListener extends AbstractDistributeOnceElasticJobListener {
    @Resource
    private TaskRepository taskRepository;

    public ElasticJobListener(long startedTimeoutMilliseconds, long completedTimeoutMilliseconds) {
        super(startedTimeoutMilliseconds, completedTimeoutMilliseconds);
    }

    @Override
    public void doBeforeJobExecutedAtLastStarted(ShardingContexts shardingContexts) {
    }

    @Override
    public void doAfterJobExecutedAtLastCompleted(ShardingContexts shardingContexts) {
        //任务执行完成后更新状态为已执行
        JobTask jobTask = taskRepository.findOne(Long.valueOf(shardingContexts.getJobParameter()));
        jobTask.setStatus(1);
        taskRepository.save(jobTask);
    }
}
