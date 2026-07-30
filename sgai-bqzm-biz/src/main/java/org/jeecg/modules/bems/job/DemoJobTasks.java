package org.jeecg.modules.bems.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 示例任务类 - 用于动态定时任务演示
 * 
 * 此类的 beanName 为 "demoJobTasks"，里面的每个 public void 无参方法都可作为定时任务执行入口。
 * 通过前端 API 添加任务时，beanName 填 "demoJobTasks"，methodName 填对应的方法名即可。
 */
@Slf4j
@Component("demoJobTasks")
public class DemoJobTasks {

    /**
     * 示例任务1：打印日志
     * 可用的 cron: "0/30 * * * * ?" (每30秒执行一次)
     */
    public void printLog() {
        log.info("===== 动态定时任务执行：printLog =====");
    }

    /**
     * 示例任务2：模拟数据同步
     * 可用的 cron: "0 0/5 * * * ?" (每5分钟执行一次)
     */
    public void syncData() {
        log.info("===== 动态定时任务执行：syncData - 开始模拟数据同步 =====");
        try {
            Thread.sleep(1000); // 模拟耗时操作
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("===== 动态定时任务执行：syncData - 数据同步完成 =====");
    }

    /**
     * 示例任务3：生成报告
     * 可用的 cron: "0 0 2 * * ?" (每天凌晨2点执行)
     */
    public void generateReport() {
        log.info("===== 动态定时任务执行：generateReport - 开始生成报告 =====");
        // 在此处编写生成报告的业务逻辑
        log.info("===== 动态定时任务执行：generateReport - 报告生成完成 =====");
    }
}
