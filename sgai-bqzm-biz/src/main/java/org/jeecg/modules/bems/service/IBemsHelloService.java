package org.jeecg.modules.bems.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bems.entity.BemsHelloEntity;

/**
 * 测试接口
 */
public interface IBemsHelloService extends IService<BemsHelloEntity> {

    String hello();

}
