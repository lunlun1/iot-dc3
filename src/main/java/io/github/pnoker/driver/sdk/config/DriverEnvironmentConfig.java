/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.driver.sdk.config;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import io.github.pnoker.common.constant.common.EnvironmentConstant;
import io.github.pnoker.common.utils.EnvironmentUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

/**
 * Driver Environment Config
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
public class DriverEnvironmentConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String node = environment.getProperty(EnvironmentConstant.DRIVER_NODE, String.class);
        if (CharSequenceUtil.isEmpty(node)) {
            node = EnvironmentUtil.getNodeId();
        }

        String tenant = environment.getProperty(EnvironmentConstant.DRIVER_TENANT, String.class);
        String name = environment.getProperty(EnvironmentConstant.SPRING_APPLICATION_NAME, String.class);
        if (CharSequenceUtil.isEmpty(name)) {
            name = StrUtil.format("{}/{}", tenant, name);
        }

        Map<String, Object> propertySourceMap = new HashMap<>(2);
        propertySourceMap.put(EnvironmentConstant.DRIVER_NODE, node);
        propertySourceMap.put(EnvironmentConstant.SPRING_APPLICATION_NAME, name);
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("driver", propertySourceMap));
    }

}
