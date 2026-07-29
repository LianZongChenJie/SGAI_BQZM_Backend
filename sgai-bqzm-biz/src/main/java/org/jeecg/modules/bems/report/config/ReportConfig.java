package org.jeecg.modules.bems.report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "report-info")
@Data
public class ReportConfig {

    private String templatePath;

    private Map<Long,String> templateDict;

}
