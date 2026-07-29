package org.jeecg.modules.bems.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.alarm.entity.AlarmCategory;
import org.jeecg.modules.bems.alarm.entity.AlarmRules;
import org.jeecg.modules.bems.alarm.mapper.AlarmCategoryMapper;
import org.jeecg.modules.bems.alarm.service.IAlarmCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlarmCategoryServiceImpl extends ServiceImpl<AlarmCategoryMapper, AlarmCategory> implements IAlarmCategoryService {
    @Override
    public IPage<AlarmCategory> listPage(AlarmCategory params) {
        return page(
                new Page<AlarmCategory>(params.getPageNo(), params.getPageSize()),
                new LambdaQueryWrapper<AlarmCategory>()
                        .like(params.getAlarmCategoryName() != null, AlarmCategory::getAlarmCategoryName, params.getAlarmCategoryName())
                        .like(params.getAlarmCategoryCode() != null, AlarmCategory::getAlarmCategoryCode, params.getAlarmCategoryCode())
                        .orderByAsc(AlarmCategory::getSort)
        );
    }

    @Override
    public void startCategory(Long id) {
        update(new LambdaUpdateWrapper<AlarmCategory>().set(AlarmCategory::getStatus, AlarmCategory.STATUS_ENABLE).eq(AlarmCategory::getId, id));
    }

    @Override
    public void stopCategory(Long id) {
        update(new LambdaUpdateWrapper<AlarmCategory>().set(AlarmCategory::getStatus, AlarmCategory.STATUS_DISABLE).eq(AlarmCategory::getId, id));
    }

    @Override
    public boolean save(AlarmCategory entity) {
        entity.setId(null);
        check(entity);
        entity.setStatus(AlarmCategory.STATUS_DISABLE);
        return super.save(entity);
    }

    @Override
    public boolean updateById(AlarmCategory entity) {
        check(entity);
        entity.setStatus(null);
        return super.updateById(entity);
    }

    @Override
    public List<AlarmCategory> list() {
        return list(new LambdaQueryWrapper<AlarmCategory>()
                .eq(AlarmCategory::getStatus, AlarmCategory.STATUS_ENABLE)
                .orderByAsc(AlarmCategory::getSort));
    }


    private void check(AlarmCategory entity) {
        checkStatus(entity.getId());
        if (count(new LambdaQueryWrapper<AlarmCategory>().eq(AlarmCategory::getAlarmCategoryCode, entity.getAlarmCategoryCode()).ne(entity.getId() != null, AlarmCategory::getId, entity.getId())) > 0) {
            throw new JeecgBootException("已存在相同编号的告警类别");
        }
        if (count(new LambdaQueryWrapper<AlarmCategory>().eq(AlarmCategory::getAlarmCategoryName, entity.getAlarmCategoryName()).ne(entity.getId() != null, AlarmCategory::getId, entity.getId())) > 0) {
            throw new JeecgBootException("已存在相同名称的告警类别");
        }
    }

    private void checkStatus(Long id) {
        if (id == null) {
            return;
        }
        AlarmCategory byId = getById(id);
        if (byId == null) {
            throw new JeecgBootException("告警类别不存在");
        }
        if (byId.getStatus().equals(AlarmCategory.STATUS_ENABLE)) {
            throw new JeecgBootException("该记录已启用，禁止操作");
        }
    }

}
