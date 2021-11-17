package io.shulie.takin.cloud.data.dao.scene.manage.impl;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.apache.commons.collections4.CollectionUtils;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.shulie.takin.cloud.data.util.MPUtil;
import io.shulie.takin.cloud.common.utils.CloudPluginUtils;
import io.shulie.takin.cloud.data.model.mysql.SceneManageEntity;
import io.shulie.takin.cloud.data.mapper.mysql.SceneManageMapper;
import io.shulie.takin.cloud.data.dao.scene.manage.SceneManageDAO;
import io.shulie.takin.cloud.data.result.scenemanage.SceneManageResult;
import io.shulie.takin.cloud.common.bean.scenemanage.SceneManageQueryBean;
import io.shulie.takin.cloud.data.result.scenemanage.SceneManageListResult;
import io.shulie.takin.cloud.data.converter.senemange.SceneManageEntityConverter;
import io.shulie.takin.cloud.data.param.scenemanage.SceneManageCreateOrUpdateParam;

/**
 * @author 无涯
 * @date 2020/10/26 4:40 下午
 */
@Component
public class SceneManageDAOImpl
    extends ServiceImpl<SceneManageMapper, SceneManageEntity>
    implements SceneManageDAO, MPUtil<SceneManageEntity> {

    @Override
    public Long insert(SceneManageCreateOrUpdateParam createParam) {
        SceneManageEntity entity = new SceneManageEntity();
        BeanUtils.copyProperties(createParam, entity);
        entity.setUserId(CloudPluginUtils.getContext().getUserId());
        entity.setTenantId(CloudPluginUtils.getContext().getTenantId());
        entity.setEnvCode(CloudPluginUtils.getContext().getEnvCode());
        this.save(entity);
        return entity.getId();
    }

    @Override
    public void update(SceneManageCreateOrUpdateParam updateParam) {
        SceneManageEntity entity = new SceneManageEntity();
        BeanUtils.copyProperties(updateParam, entity);
        entity.setUserId(null);
        entity.setTenantId(null);
        entity.setEnvCode(null);
        this.updateById(entity);
    }

    @Override
    public SceneManageResult getSceneById(Long id) {
        SceneManageEntity entity = this.getById(id);
        if (entity != null) {return BeanUtil.copyProperties(entity, SceneManageResult.class);}
        return null;
    }

    @Override
    public List<SceneManageEntity> getPageList(SceneManageQueryBean queryBean) {
        // 补充用户过滤信息信息
        String userIds = "";
        if (StrUtil.isNotBlank(CloudPluginUtils.getContext().getFilterSql())) {
            userIds = CloudPluginUtils.getContext().getFilterSql();
            // 去除左右的括号
            if (userIds.lastIndexOf("(") == 0
                && userIds.lastIndexOf(")") == userIds.length() - 1) {
                userIds = userIds.substring(1, userIds.length() - 1);
            }
        }
        List<String> userIdList = Arrays.stream(userIds.split(","))
            .filter(StrUtil::isNotBlank).collect(Collectors.toList());
        // 组装查询条件
        LambdaQueryWrapper<SceneManageEntity> wrapper = Wrappers.lambdaQuery(SceneManageEntity.class)
            .eq(!Objects.isNull(queryBean.getSceneId()), SceneManageEntity::getId, queryBean.getSceneId())
            .in(!CollectionUtils.isEmpty(queryBean.getSceneIds()), SceneManageEntity::getId, queryBean.getSceneIds())
            .like(!StrUtil.isBlank(queryBean.getSceneName()), SceneManageEntity::getSceneName, queryBean.getSceneName())
            .eq(!Objects.isNull(queryBean.getStatus()), SceneManageEntity::getStatus, queryBean.getStatus())
            .eq(!Objects.isNull(queryBean.getType()), SceneManageEntity::getType, queryBean.getType())
            .le(!Objects.isNull(queryBean.getLastPtEndTime()), SceneManageEntity::getLastPtTime, queryBean.getLastPtEndTime())
            .ge(!Objects.isNull(queryBean.getLastPtStartTime()), SceneManageEntity::getLastPtTime, queryBean.getLastPtStartTime())
            .eq(SceneManageEntity::getTenantId, CloudPluginUtils.getContext().getTenantId())
            .eq(SceneManageEntity::getEnvCode, CloudPluginUtils.getContext().getEnvCode())
            .in(userIdList.size() > 0, SceneManageEntity::getUserId, userIdList)
            .orderByDesc(SceneManageEntity::getLastPtTime)
            .orderByDesc(SceneManageEntity::getId);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public SceneManageListResult queryBySceneName(String pressureTestSceneName) {
        LambdaQueryWrapper<SceneManageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SceneManageEntity::getSceneName, pressureTestSceneName);
        SceneManageEntity sceneManageEntity = this.getBaseMapper().selectOne(wrapper);
        return SceneManageEntityConverter.INSTANCE.ofSceneManageEntity(sceneManageEntity);
    }

    @Override
    public List<SceneManageEntity> listFromUpdateScript() {
        return this.list(this.getTenantLQW().select(SceneManageEntity::getId,
            SceneManageEntity::getTenantId, SceneManageEntity::getFeatures));
    }

    @Override
    public SceneManageListResult querySceneManageById(Long sceneId) {
        LambdaQueryWrapper<SceneManageEntity> wrapper = new LambdaQueryWrapper<>();
        if (sceneId != null) {
            wrapper.eq(SceneManageEntity::getId, sceneId);
        }
        SceneManageEntity sceneManageEntities = this.getBaseMapper().selectOne(wrapper);
        return SceneManageEntityConverter.INSTANCE.ofSceneManageEntity(sceneManageEntities);
    }

    @Override
    public SceneManageEntity queueSceneById(Long sceneId) {
        LambdaQueryWrapper<SceneManageEntity> wrapper = new LambdaQueryWrapper<>();
        if (sceneId != null) {
            wrapper.eq(SceneManageEntity::getId, sceneId);
        }
        return this.getBaseMapper().selectOne(wrapper);
    }

    /**
     * 根据场景主键设置场景状态
     *
     * @param sceneId 场景主键
     * @param status  状态值
     * @return 操作影响行数
     */
    @Override
    public int updateStatus(Long sceneId, Integer status) {
        return this.baseMapper.update(
            new SceneManageEntity() {{setStatus(status);}},
            Wrappers.lambdaQuery(SceneManageEntity.class).eq(SceneManageEntity::getId, sceneId));
    }

    /**
     * 根据场景主键设置场景状态
     *
     * @param sceneId       场景主键
     * @param status        状态值
     * @param compareStatus （CAS操作）需要比较的状态值，为空则不进行比较
     * @return 操作影响行数
     */
    @Override
    public int updateStatus(Long sceneId, Integer status, Integer compareStatus) {
        LambdaQueryWrapper<SceneManageEntity> wrapper = Wrappers.lambdaQuery(SceneManageEntity.class)
            .eq(!Objects.isNull(sceneId), SceneManageEntity::getId, sceneId)
            .eq(!Objects.isNull(compareStatus), SceneManageEntity::getStatus, compareStatus);
        return this.baseMapper.update(new SceneManageEntity() {{setStatus(status);}}, wrapper);
    }
}
