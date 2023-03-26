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

import io.github.pnoker.common.dto.DriverRegisterDTO;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.model.Driver;
import io.github.pnoker.driver.sdk.DriverContext;
import io.github.pnoker.driver.sdk.property.DriverProperty;
import io.github.pnoker.driver.sdk.service.DriverRegisterService;
import io.github.pnoker.driver.sdk.service.DriverService;
import io.github.pnoker.common.utils.HostUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 驱动注册接口实现
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverRegisterServiceImpl implements DriverRegisterService {

    @Value("${server.port:0}")
    private int port;
    @Value("${spring.application.name}")
    private String serviceName;

    @Resource
    private DriverService driverService;
    @Resource
    private DriverProperty driverProperty;
    @Resource
    private DriverContext driverContext;

    @Override
    public void register() {
        log.info("The driver {}/{} is initializing", this.serviceName, driverProperty.getName());

        try {
            Driver driver = new Driver();
            driver.setDriverName(driverProperty.getName());
            driver.setServiceName(this.serviceName);
            driver.setServiceHost(HostUtil.localHost());
            driver.setServicePort(this.port);
            driver.setDriverTypeFlag(driverProperty.getType());
            driver.setRemark(driverProperty.getRemark());

            DriverRegisterDTO driverRegisterDTO = new DriverRegisterDTO(
                    driverProperty.getTenant(),
                    driver,
                    driverProperty.getDriverAttribute(),
                    driverProperty.getPointAttribute()
            );
            driverService.driverRegisterSender(driverRegisterDTO);
        } catch (Exception ignored) {
            driverService.close("The driver initialization failed, registration timed out.");
            Thread.currentThread().interrupt();
        }

        driverContext.setDriverStatus(DriverStatusEnum.ONLINE);
        log.info("The driver {}/{} is initialized successfully.", this.serviceName, driverProperty.getName());
    }
}
