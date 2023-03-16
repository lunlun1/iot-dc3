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

package io.github.pnoker.common.sdk.config;

import io.github.pnoker.common.config.TopicConfig;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.utils.EnvironmentUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(TopicConfig.class)
public class DriverTopicConfig {
    @Value("${spring.env:}")
    private String env;
    @Value("${spring.application.name}")
    private String serviceName;

    @Resource
    private TopicExchange eventExchange;
    @Resource
    private TopicExchange metadataExchange;

    @Bean
    Queue driverMetadataQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // 30秒：30 * 1000 = 30000L
        arguments.put(RabbitConstant.MESSAGE_TTL, 30000L);
        return new Queue(RabbitConstant.QUEUE_DRIVER_METADATA_PREFIX + this.serviceName, false, false, EnvironmentUtil.isDev(env), arguments);
    }

    @Bean
    Binding metadataBinding(Queue driverMetadataQueue) {
        return BindingBuilder
                .bind(driverMetadataQueue)
                .to(metadataExchange)
                .with(RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + this.serviceName);
    }

}
