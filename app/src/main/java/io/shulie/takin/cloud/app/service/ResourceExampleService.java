package io.shulie.takin.cloud.app.service;

/**
 * 资源实例服务
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
public interface ResourceExampleService {
    /**
     * 心跳事件
     *
     * @param id 资源实例主键
     */
    void onHeartbeat(long id);

    /**
     * 启动事件
     *
     * @param id 资源实例主键
     */
    void onStart(long id);

    /**
     * 停止事件
     *
     * @param id 资源实例主键
     */
    void onStop(long id);

    /**
     * 异常事件
     *
     * @param id        资源实例主键
     * @param errorInfo 错误信息
     */
    void onError(long id, String errorInfo);
}
