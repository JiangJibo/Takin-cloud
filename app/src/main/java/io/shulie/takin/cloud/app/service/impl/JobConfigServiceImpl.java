package io.shulie.takin.cloud.app.service.impl;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.shulie.takin.cloud.constant.enums.ThreadGroupType;

import io.shulie.takin.cloud.app.entity.MetricsEntity;
import io.shulie.takin.cloud.app.service.JobConfigService;
import io.shulie.takin.cloud.app.entity.ThreadConfigEntity;
import io.shulie.takin.cloud.app.entity.ThreadConfigExampleEntity;
import io.shulie.takin.cloud.app.service.mapper.MetricsMapperService;
import io.shulie.takin.cloud.app.service.impl.mapper.ThreadConfigMapperServiceImpl;
import io.shulie.takin.cloud.app.service.impl.mapper.ThreadConfigExampleMapperServiceImpl;

/**
 * 任务配置服务 - 实例
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Slf4j
@Service
public class JobConfigServiceImpl implements JobConfigService {
    @javax.annotation.Resource
    MetricsMapperService metricsMapperService;
    @javax.annotation.Resource
    ThreadConfigMapperServiceImpl threadConfigMapperService;
    @javax.annotation.Resource
    ThreadConfigExampleMapperServiceImpl threadConfigExampleMapperService;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetricsEntity> metricsList(long jobId) {
        return metricsMapperService.lambdaQuery()
            .eq(MetricsEntity::getJobId, jobId)
            .list();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ThreadConfigExampleEntity> threadList(long jobId) {
        return threadConfigExampleMapperService.lambdaQuery()
            .eq(ThreadConfigExampleEntity::getJobId, jobId)
            .list();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ThreadConfigExampleEntity> threadExampleItem(long jobId, String ref) {
        return threadConfigExampleMapperService.lambdaQuery()
            .eq(ThreadConfigExampleEntity::getJobId, jobId)
            .eq(StrUtil.isNotBlank(ref), ThreadConfigExampleEntity::getRef, ref)
            .list();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifThreadConfigExample(long threadConfigExampleId, ThreadGroupType type, String context) {
        threadConfigExampleMapperService.updateById(new ThreadConfigExampleEntity() {{
            setId(threadConfigExampleId);
            setType(type);
            setContext(context);
        }});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createMetrics(List<MetricsEntity> metricsEntityList) {
        metricsMapperService.saveBatch(metricsEntityList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createThread(List<ThreadConfigEntity> metricsEntityList) {
        threadConfigMapperService.saveBatch(metricsEntityList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createThreadExample(List<ThreadConfigExampleEntity> threadConfigExampleEntityList) {
        threadConfigExampleMapperService.saveBatch(threadConfigExampleEntityList);
    }
}
