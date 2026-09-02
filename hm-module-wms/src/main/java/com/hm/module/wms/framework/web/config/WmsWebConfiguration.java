package com.hm.module.wms.framework.web.config;

import com.hm.framework.swagger.config.HmSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WMS 模块的 web 组件的 Configuration
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
public class WmsWebConfiguration {

    /**
     * WMS 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi wmsGroupedOpenApi() {
        return HmSwaggerAutoConfiguration.buildGroupedOpenApi("wms");
    }

}
