package org.jeecg.modules.bems.patterned.mq.listener;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.bems.patterned.mq.constant.PatterningMqConstant;
import org.jeecg.modules.bems.patterned.dto.PatterningDelayMessage;
import org.jeecg.modules.bems.patterned.entity.PatterningExecutionTime;
import org.jeecg.modules.bems.patterned.service.IPatterningExecutionTimeService;
import org.jeecg.modules.bems.patterned.service.IPatterningStrategyService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 场景控制延迟消息消费者
 */
@Component
@Slf4j
@AllArgsConstructor
public class PatterningStrategyListener {

    private final IPatterningStrategyService patterningStrategyService;
    private final IPatterningExecutionTimeService patterningExecutionTimeService;

    @RabbitListener(queues = PatterningMqConstant.QUEUE_PATTERNING_EXECUTE)
    public void processPatterningExecute(Message message) {
        try {
            PatterningDelayMessage msg = JSONObject.parseObject(
                    new String(message.getBody()),
                    PatterningDelayMessage.class
            );

            log.info("收到场景控制延迟消息: patterningId={}, executeTime={}, version={}",
                    msg.getId(), msg.getExecuteTime(), msg.getVersion());

            // 核对版本号
            PatterningExecutionTime config = patterningExecutionTimeService.getByPatterningId(msg.getId());
            if (config == null || !msg.getVersion().equals(config.getVersion())) {
                log.warn("场景控制版本号不匹配，丢弃消息: patterningId={}, msgVersion={}, currentVersion={}",
                        msg.getId(), msg.getVersion(), config != null ? config.getVersion() : "null");
                return;
            }

            // 调用执行方法（包含时间校验）
            patterningStrategyService.executeImmediately(msg.getId(), msg.getExecuteTime());

            log.info("场景控制执行成功: patterningId={}", msg.getId());

        } catch (JeecgBootException e) {
            // 业务异常，不重试
            log.error("场景控制执行业务异常: {}", e.getMessage());
        } catch (Exception e) {
            // 系统异常，抛出触发重试
            log.error("场景控制执行系统异常，将重试", e);
        }
    }
}
