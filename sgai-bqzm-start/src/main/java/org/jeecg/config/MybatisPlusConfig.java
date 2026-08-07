package org.jeecg.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置 —— 强制关闭 SQL 日志输出。
 * <p>
 * 覆盖 Nacos 远程配置可能设置的 StdOutImpl，作为终极兜底。
 * 无论远程/本地/启动参数怎么配，SQL 日志都会被关闭。
 * <p>
 * 如需临时排查 SQL，注释掉此 Bean 重启即可。
 *
 * @see ConfigurationCustomizer
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public ConfigurationCustomizer disableSqlLogCustomizer() {
        return configuration -> configuration.setLogImpl(NoLoggingImpl.class);
    }
}
