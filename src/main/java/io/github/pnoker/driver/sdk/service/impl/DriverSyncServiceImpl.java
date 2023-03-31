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
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.dto.DriverRegisterDTO;
import io.github.pnoker.common.dto.DriverSyncDTO;
import io.github.pnoker.common.entity.driver.DriverMetadata;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.model.Driver;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.driver.sdk.DriverContext;
import io.github.pnoker.driver.sdk.property.DriverProperty;
import io.github.pnoker.driver.sdk.service.DriverSyncService;
import io.github.pnoker.driver.sdk.service.DriverSenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 驱动注册接口实现
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverSyncServiceImpl implements DriverSyncService {

    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverProperty driverProperty;

    @Resource
    private DriverSenderService driverSenderService;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void up() {
        try {
            DriverRegisterDTO driverRegisterDTO = buildRegisterDTOByProperty();

            log.debug("The driver {} initialization information is: {}", driverProperty.getService(), JsonUtil.toPrettyJsonString(driverRegisterDTO));
            driverSenderService.driverRegisterSender(driverRegisterDTO);

            log.info("The driver {} is initializing", driverProperty.getService());
            threadPoolExecutor.submit(() -> {
                while (!DriverStatusEnum.ONLINE.equals(driverContext.getDriverStatus())) {
                    ThreadUtil.sleep(500);
                }
            }).get(15, TimeUnit.SECONDS);

            log.info("The driver {} is initialized successfully.", driverProperty.getService());
        } catch (Exception ignored) {
            log.error("The driver initialization failed, registration response timed out.");
            System.exit(1);
        }
    }

    @Override
    public void down(DriverSyncDTO entityDTO) {
        if (ObjectUtil.isNull(entityDTO.getContent())) {
            return;
        }

        if (CharSequenceUtil.isEmpty(entityDTO.getContent())) {
            return;
        }
        DriverMetadata driverMetadata = JsonUtil.parseObject(entityDTO.getContent(), DriverMetadata.class);
        if (ObjectUtil.isNull(driverMetadata)) {
            driverMetadata = new DriverMetadata();
        }
        driverContext.setDriverMetadata(driverMetadata);
        driverContext.setDriverStatus(DriverStatusEnum.ONLINE);
        driverMetadata.getDriverAttributeMap().values().forEach(driverAttribute -> log.info("Syncing driver attribute[{}] metadata: {}", driverAttribute.getAttributeName(), JsonUtil.toPrettyJsonString(driverAttribute)));
        driverMetadata.getPointAttributeMap().values().forEach(pointAttribute -> log.info("Syncing point attribute[{}] metadata: {}", pointAttribute.getAttributeName(), JsonUtil.toPrettyJsonString(pointAttribute)));
        driverMetadata.getDeviceMap().values().forEach(device -> log.info("Syncing device[{}] metadata: {}", device.getDeviceName(), JsonUtil.toPrettyJsonString(device)));
        log.info("The metadata synced successfully.");
    }

    /**
     * Property To DriverRegisterDTO
     *
     * @return DriverRegisterDTO
     */
    private DriverRegisterDTO buildRegisterDTOByProperty() {
        DriverRegisterDTO driverRegisterDTO = new DriverRegisterDTO();
        driverRegisterDTO.setTenant(driverProperty.getTenant());
        driverRegisterDTO.setClient(driverProperty.getClient());
        Driver driver = buildDriverByProperty();
        driverRegisterDTO.setDriver(driver);
        driverRegisterDTO.setDriverAttributes(driverProperty.getDriverAttribute());
        driverRegisterDTO.setPointAttributes(driverProperty.getPointAttribute());
        return driverRegisterDTO;
    }

    /**
     * Property To Driver
     *
     * @return Driver
     */
    private Driver buildDriverByProperty() {
        Driver driver = new Driver();
        driver.setDriverName(driverProperty.getName());
        driver.setDriverCode(driverProperty.getCode());
        driver.setServiceName(driverProperty.getService());
        driver.setServiceHost(driverProperty.getHost());
        driver.setServicePort(driverProperty.getPort());
        driver.setDriverTypeFlag(driverProperty.getType());
        driver.setRemark(driverProperty.getRemark());
        return driver;
    }

}
