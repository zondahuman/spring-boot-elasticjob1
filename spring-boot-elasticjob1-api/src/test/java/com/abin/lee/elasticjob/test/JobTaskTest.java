package com.abin.lee.elasticjob.test;

import com.abin.lee.elasticjob.dao.TaskRepository;
import com.abin.lee.elasticjob.entity.JobTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * Created by IntelliJ IDEA.
 *
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class JobTaskTest {
    @Resource
    private TaskRepository taskRepository;

    @Test
    public void add() {
        //生成几个任务，第一任务在三分钟之后
        Long unixTime = System.currentTimeMillis() + 60000;
        JobTask task = new JobTask("test-msg-1", 0, unixTime);
        taskRepository.save(task);
        unixTime += 60000;
        task = new JobTask("test-msg-2", 0, unixTime);
        taskRepository.save(task);
        unixTime += 60000;
        task = new JobTask("test-msg-3", 0, unixTime);
        taskRepository.save(task);
        unixTime += 60000;
        task = new JobTask("test-msg-4", 0, unixTime);
        taskRepository.save(task);
    }
}
