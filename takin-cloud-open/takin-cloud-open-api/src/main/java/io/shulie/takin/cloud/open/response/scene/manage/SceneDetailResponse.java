package io.shulie.takin.cloud.open.response.scene.manage;

import java.util.Map;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import io.shulie.takin.cloud.open.request.scene.manage.WriteSceneRequest;
import io.shulie.takin.cloud.open.request.scene.manage.WriteSceneRequest.Goal;
import io.shulie.takin.cloud.open.request.scene.manage.WriteSceneRequest.Config;
import io.shulie.takin.cloud.open.request.scene.manage.WriteSceneRequest.Content;
import io.shulie.takin.cloud.open.request.scene.manage.WriteSceneRequest.MonitoringGoal;
import io.shulie.takin.cloud.open.request.scene.manage.WriteSceneRequest.DataValidation;

/**
 * 场景详情  -  响应
 *
 * @author 张天赐
 */
@Data
@ApiModel(value = "场景详情")
public class SceneDetailResponse {
    @ApiModelProperty(value = "基础信息")
    @NotBlank(message = "场景基础信息不能为空")
    private WriteSceneRequest.BasicInfo basicInfo;
    @ApiModelProperty(value = "脚本解析结果")
    @NotBlank(message = "脚本解析结果不能为空")
    private List<?> analysisResult;
    @ApiModelProperty(value = "压测内容")
    @NotNull(message = "压测目标不能为空")
    private List<Content> content;
    @ApiModelProperty(value = "施压配置")
    @NotNull(message = "施压配置不能为空")
    private Map<String, Config> config;
    @ApiModelProperty(value = "压测目标")
    @NotNull(message = "业压测目标不能为空")
    private Map<String, Goal> goal;
    @ApiModelProperty(value = "SLA配置")
    @NotNull(message = "SLA配置不能为空")
    private List<MonitoringGoal> monitoringGoal;
    @ApiModelProperty(value = "数据验证配置")
    @NotNull(message = "数据验证配置不能为空")
    private DataValidation dataValidation;
}