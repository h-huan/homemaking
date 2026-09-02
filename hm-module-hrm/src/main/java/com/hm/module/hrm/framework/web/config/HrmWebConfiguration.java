package com.hm.module.hrm.framework.web.config;

import com.hm.framework.swagger.config.HmSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * hrm 模块的 web 组件的 Configuration
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
public class HrmWebConfiguration {

    /**
     * hrm 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi hrmGroupedOpenApi() {
        return HmSwaggerAutoConfiguration.buildGroupedOpenApi("hrm");
    }

}
