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

package io.github.pnoker.driver.sdk.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.constant.driver.EventConstant;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.dto.DriverEventDTO;
import io.github.pnoker.common.dto.DriverRegisterDTO;
import io.github.pnoker.common.entity.DeviceEvent;
import io.github.pnoker.common.entity.point.PointValue;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.driver.sdk.DriverContext;
import io.github.pnoker.driver.sdk.property.DriverProperty;
import io.github.pnoker.driver.sdk.service.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {

    @Resource
    private DriverProperty driverProperty;
    @Resource
    private DriverContext driverContext;

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void driverRegisterSender(DriverRegisterDTO entityDTO) {
        if (ObjectUtil.isNull(entityDTO)) {
            return;
        }

        rabbitTemplate.convertAndSend(
                RabbitConstant.TOPIC_EXCHANGE_REGISTER,
                RabbitConstant.ROUTING_DRIVER_REGISTER_PREFIX + driverProperty.getClient(),
                entityDTO
        );
    }

    @Override
    public void driverEventSender(DriverEventDTO entityDTO) {
        if (ObjectUtil.isNull(entityDTO)) {
            return;
        }

        rabbitTemplate.convertAndSend(
                RabbitConstant.TOPIC_EXCHANGE_EVENT,
                RabbitConstant.ROUTING_DRIVER_EVENT_PREFIX + driverContext.getDriverMetadata().getDriverId(),
                entityDTO
        );
    }

    @Override
    public void deviceEventSender(DeviceEvent deviceEvent) {
        if (ObjectUtil.isNotNull(deviceEvent)) {
            rabbitTemplate.convertAndSend(
                    RabbitConstant.TOPIC_EXCHANGE_EVENT,
                    RabbitConstant.ROUTING_DEVICE_EVENT_PREFIX + driverContext.getDriverMetadata().getDriverId(),
                    deviceEvent
            );
        }
    }

    @Override
    public void deviceStatusSender(String deviceId, DriverStatusEnum status) {
        deviceEventSender(new DeviceEvent(deviceId, EventConstant.Device.STATUS, status));
    }

    @Override
    public void pointValueSender(PointValue pointValue) {
        if (ObjectUtil.isNotNull(pointValue)) {
            log.debug("Send point value: {}", JsonUtil.toJsonString(pointValue));
            rabbitTemplate.convertAndSend(
                    RabbitConstant.TOPIC_EXCHANGE_VALUE,
                    RabbitConstant.ROUTING_POINT_VALUE_PREFIX + driverContext.getDriverMetadata().getDriverId(),
                    pointValue
            );
        }
    }

    @Override
    public void pointValueSender(List<PointValue> pointValues) {
        if (ObjectUtil.isNotNull(pointValues)) {
            pointValues.forEach(this::pointValueSender);
        }
    }

    @Override
    public void close(CharSequence template, Object... params) {
        log.error(CharSequenceUtil.format(template, params));
        ((ConfigurableApplicationContext) applicationContext).close();
        System.exit(1);
    }

}
