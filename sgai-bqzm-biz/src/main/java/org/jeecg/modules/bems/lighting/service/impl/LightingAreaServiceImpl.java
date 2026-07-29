package org.jeecg.modules.bems.lighting.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.lighting.dto.LightingAreaQueryDto;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingOperationLog;
import org.jeecg.modules.bems.lighting.mapper.LightingAreaMapper;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingOperationLogService;
import org.jeecg.modules.bems.lighting.service.LightingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


@Service
@AllArgsConstructor
@Slf4j
public class LightingAreaServiceImpl extends ServiceImpl<LightingAreaMapper, LightingArea> implements ILightingAreaService {

    private final LightingService service;

    private final ILightingOperationLogService lightingOperationLogService;

    @Override
    public IPage<LightingArea> listPage(LightingAreaQueryDto params) {
        IPage<LightingArea> page = listPage1(params);
        if(CollectionUtil.isEmpty(page.getRecords())){
            return page;
        }
        List<LightingArea> records = page.getRecords();
        for(LightingArea area : records){
            area.setAreaName(area.getSpaceName() + area.getAreaName());
        }
        return page;
    }

    @Override
    public IPage<LightingArea> listPage1(LightingAreaQueryDto params) {
        LambdaQueryWrapper<LightingArea> queryWrapper = new LambdaQueryWrapper<LightingArea>()
                .like(StringUtils.isNotEmpty(params.getRelName()),LightingArea::getRelName, params.getRelName())
                .eq(StringUtils.isNotEmpty(params.getSpace()),LightingArea::getSpace,params.getSpace())
                .like(StringUtils.isNotEmpty(params.getAreaName()),LightingArea::getAreaName, params.getAreaName())
                .orderByAsc(LightingArea::getSort);
        return super.page(new Page<>(params.getPageNo(), params.getPageSize()),queryWrapper);
    }

    /**
     * 区域-全开
     * @param id 区域id
     * @return true-成功，false-失败
     */
    @Override
    @Transactional
    public void open(Long id) {
        control(id,true);
    }

    /**
     * 区域-全关
     * @param id 区域id
     * @return true-成功，false-失败
     */
    @Override
    @Transactional
    public void close(Long id) {
        control(id,false);
    }

    /**
     * mq-状态监听
     * @param areaCode 区域编码
     */
    @Override
    public void mqControl(String space,String areaCode,String value) {
        LightingArea area = super.getOne(new LambdaQueryWrapper<LightingArea>().eq(LightingArea::getSpace, space).eq(LightingArea::getAreaCode, areaCode));
        if(area == null){
            return;
        }
        // 判断状态
        String status = "";
        if(value.equals(area.getOpenCode())){
            status = "开启";
        }else if(value.equals(area.getCloseCode())){
            status = "关闭";
        }else {
            log.error("照明区域场景状态错误。space: {},areaCode: {},value: {}",space,areaCode,value);
            return;
        }

        super.update(new LambdaUpdateWrapper<LightingArea>().eq(LightingArea::getId,area.getId()).set(LightingArea::getStatus,status));
    }

    @Override
    public LightingArea getByCode(String space,String areaCode) {
        return getOne(new LambdaQueryWrapper<LightingArea>().eq(LightingArea::getSpace,space).eq(LightingArea::getAreaCode,areaCode));
    }

    @Override
    public List<LightingArea> getByIds(Collection<Long> ids) {
        if(CollectionUtil.isEmpty(ids)){
            return Collections.emptyList();
        }
        return super.list(new LambdaQueryWrapper<LightingArea>().in(LightingArea::getId,ids));
    }

    private void control(Long id, boolean type){
        LightingArea area = super.getById(id);
        if(area == null){
            throw new JeecgBootException("区域不存在");
        }
        if( type) {
            service.areaOpen(area.getSpace(),area.getAreaCode(),area.getOpenCode());
            lightingOperationLogService.saveLog(LightingOperationLog.REL_TYPE_AREA,id,area.getAreaName(), LocalDateTime.now(),"区域全开");
        }else {
            service.areaClose(area.getSpace(),area.getAreaCode(),area.getCloseCode());
            lightingOperationLogService.saveLog(LightingOperationLog.REL_TYPE_AREA,id,area.getAreaName(), LocalDateTime.now(),"区域全关");
        }
    }
}
