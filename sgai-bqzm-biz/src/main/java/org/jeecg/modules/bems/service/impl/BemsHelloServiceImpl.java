package org.jeecg.modules.bems.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bems.entity.BemsHelloEntity;
import org.jeecg.modules.bems.mapper.BemsHelloMapper;
import org.jeecg.modules.bems.service.IBemsHelloService;
import org.springframework.stereotype.Service;

/**
 * 测试Service
 */
@Service
public class BemsHelloServiceImpl extends ServiceImpl<BemsHelloMapper, BemsHelloEntity> implements IBemsHelloService {

    @Override
    public String hello() {
        return "hello ，我是 bems 微服务节点!";
    }
}
