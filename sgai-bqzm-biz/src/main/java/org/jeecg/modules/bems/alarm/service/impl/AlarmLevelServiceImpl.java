package org.jeecg.modules.bems.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.alarm.entity.AlarmLevel;
import org.jeecg.modules.bems.alarm.mapper.AlarmLevelMapper;
import org.jeecg.modules.bems.alarm.service.IAlarmLevelService;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
public class AlarmLevelServiceImpl extends ServiceImpl<AlarmLevelMapper, AlarmLevel> implements IAlarmLevelService {

    @Override
    public boolean save(AlarmLevel entity) {
        check(entity);
        entity.setStatus(AlarmLevel.STATUS_DISABLE);
        return super.save(entity);
    }

    @Override
    public boolean updateById(AlarmLevel entity) {
        check(entity);
        entity.setStatus(null);
        return super.updateById(entity);
    }

    @Override
    public IPage<AlarmLevel> listPage(AlarmLevel params) {
        return page(
                new Page<>(params.getPageNo(), params.getPageSize()),
                new LambdaQueryWrapper<AlarmLevel>()
                        .like(params.getAlarmLevelName() != null, AlarmLevel::getAlarmLevelName, params.getAlarmLevelName())
                        .like(params.getAlarmLevelCode() != null, AlarmLevel::getAlarmLevelCode, params.getAlarmLevelCode())
                        .eq(StringUtils.isNotEmpty(params.getStatus()), AlarmLevel::getStatus, params.getStatus())
                        .orderByAsc(AlarmLevel::getSort)
        );
    }

    @Override
    public void startLevel(Long id) {
        update(new LambdaUpdateWrapper<AlarmLevel>().set(AlarmLevel::getStatus, AlarmLevel.STATUS_ENABLE).eq(AlarmLevel::getId, id));
    }

    @Override
    public void stopLevel(Long id) {
        update(new LambdaUpdateWrapper<AlarmLevel>().set(AlarmLevel::getStatus, AlarmLevel.STATUS_DISABLE).eq(AlarmLevel::getId, id));
    }

    @Override
    public boolean removeById(Serializable id) {
        checkStatus((Long) id);
        return super.removeById(id);
    }

    @Override
    public List<AlarmLevel> list() {
        return super.list(new LambdaQueryWrapper<AlarmLevel>()
                .eq(AlarmLevel::getStatus, 1)
                .orderByAsc(AlarmLevel::getSort));
    }

    private void check(AlarmLevel entity) {
        checkStatus(entity.getId());
        if (count(new LambdaQueryWrapper<AlarmLevel>().eq(AlarmLevel::getAlarmLevelCode, entity.getAlarmLevelCode()).ne(entity.getId() != null, AlarmLevel::getId, entity.getId())) > 0) {
            throw new JeecgBootException("已存在相同编号的报警级别");
        }
        if (count(new LambdaQueryWrapper<AlarmLevel>().eq(AlarmLevel::getAlarmLevelName, entity.getAlarmLevelName()).ne(entity.getId() != null, AlarmLevel::getId, entity.getId())) > 0) {
            throw new JeecgBootException("已存在相同名称的报警级别");
        }
    }

    private void checkStatus(Long id) {
        if (id != null) {
            AlarmLevel byId = getById(id);
            if (byId == null) {
                throw new JeecgBootException("不存在该记录");
            }
            if (byId.getStatus().equals(AlarmLevel.STATUS_ENABLE)) {
                throw new JeecgBootException("该记录已启用，禁止操作");
            }
        }
    }
}
