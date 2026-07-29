package org.jeecg.modules.bems.bc.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.bems.bc.entity.BuildingControlPoint;
import org.jeecg.modules.bems.bc.mapper.BuildingControlPointMapper;
import org.jeecg.modules.bems.bc.service.IBuildingControlPointHistoryService;
import org.jeecg.modules.bems.bc.service.IBuildingControlPointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class BuildingControlPointServiceImpl extends ServiceImpl<BuildingControlPointMapper, BuildingControlPoint> implements IBuildingControlPointService {
    private final IBuildingControlPointHistoryService historyService;

    private final RedisUtil redisUtil;

    @Override
    public Page<BuildingControlPoint> listPage(BuildingControlPoint params) {
        return super.page(new Page<>(params.getPageNo(), params.getPageSize()),new LambdaQueryWrapper<BuildingControlPoint>()
                .like(StringUtils.isNotEmpty(params.getGatewayAdr()), BuildingControlPoint::getGatewayAdr, params.getGatewayAdr())
                .like(StringUtils.isNotEmpty(params.getBacnetAdr()), BuildingControlPoint::getBacnetAdr, params.getBacnetAdr())
                        .like(StringUtils.isNotEmpty(params.getContent()),BuildingControlPoint::getContent,params.getContent())
                .orderByDesc(BuildingControlPoint::getId));
    }

    @Override
    @Transactional
    public void save(String gatewayAdr, String bacnetAdr, String value,String remark, LocalDateTime collectionTime) {
        // 增加缓存，减少数据库查询次数
        BuildingControlPoint one = getByGatewayAdrAndBacnetAdr(gatewayAdr,bacnetAdr);
        if(one == null){
            one = new BuildingControlPoint();
            one.setGatewayAdr(gatewayAdr);
            one.setBacnetAdr(bacnetAdr);
        }
        if(StrUtil.isNotEmpty(remark)){
            one.setContent(remark);
        }
        one.setValue(value);
        one.setCollectionTime(collectionTime);
        super.saveOrUpdate(one);
        redisUtil.set(getRedisKey(gatewayAdr, bacnetAdr),one);
        historyService.save(one.getId(),one.getValue(),one.getCollectionTime());
    }


    private BuildingControlPoint getByGatewayAdrAndBacnetAdr(String gatewayAdr,String bacnetAdr){
        // 缓存
        Object o = redisUtil.get(getRedisKey(gatewayAdr, bacnetAdr));
        if(o != null){
            return (BuildingControlPoint) o;
        }
        return super.getOne(new LambdaQueryWrapper<BuildingControlPoint>().eq(BuildingControlPoint::getGatewayAdr, gatewayAdr).eq(BuildingControlPoint::getBacnetAdr, bacnetAdr));
    }

    private String getRedisKey(String gatewayAdr,String bacnetAdr){
        return "bc:"+gatewayAdr+"-"+bacnetAdr;
    }

}
