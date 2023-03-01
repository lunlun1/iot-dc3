/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.sdk.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.constant.driver.EventConstant;
import io.github.pnoker.common.entity.DriverEvent;
import io.github.pnoker.common.entity.driver.AttributeInfo;
import io.github.pnoker.common.entity.driver.DriverRegister;
import io.github.pnoker.common.enums.StatusEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.*;
import io.github.pnoker.common.sdk.bean.driver.DriverContext;
import io.github.pnoker.common.sdk.bean.driver.DriverProperty;
import io.github.pnoker.common.sdk.service.DriverMetadataService;
import io.github.pnoker.common.sdk.service.DriverService;
import io.github.pnoker.common.utils.HostUtil;
import io.github.pnoker.common.utils.RegexUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Driver Metadata Service Implements
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverMetadataServiceImpl implements DriverMetadataService {

    @Value("${server.port}")
    private int port;
    @Value("${spring.application.name}")
    private String serviceName;

    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;
    @Resource
    private DriverProperty driverProperty;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void initial() {
        String localHost = HostUtil.localHost();
        if (!RegexUtil.isName(driverProperty.getName()) || !RegexUtil.isName(this.serviceName) || !RegexUtil.isHost(localHost)) {
            throw new ServiceException("The driver name, service name or host name format is invalid");
        }
        if (!RegexUtil.isDriverPort(this.port)) {
            throw new ServiceException("The driver port is invalid, port range is 8600-8799");
        }

        Driver driver = new Driver();
        driver.setDriverName(driverProperty.getName());
        driver.setServiceName(this.serviceName);
        driver.setServiceHost(localHost);
        driver.setServicePort(this.port);
        driver.setDriverTypeFlag(driverProperty.getType());
        driver.setRemark(driverProperty.getRemark());
        log.info("The driver {}/{} is initializing", driver.getServiceName(), driver.getDriverName());

        registerHandshake();
        driverService.driverEventSender(new DriverEvent(
                serviceName,
                EventConstant.Driver.REGISTER,
                new DriverRegister(
                        driverProperty.getTenant(),
                        driver,
                        driverProperty.getDriverAttribute(),
                        driverProperty.getPointAttribute()
                )
        ));
        syncDriverMetadata(driver);

        log.info("The driver {}/{} is initialized successfully", driver.getServiceName(), driver.getDriverName());
    }

    @Override
    public void upsertProfile(Profile profile) {
        // Add profile point to context
        driverContext.getDriverMetadata().getProfilePointMap().computeIfAbsent(profile.getId(), k -> new ConcurrentHashMap<>(16));
    }

    @Override
    public void deleteProfile(String id) {
        driverContext.getDriverMetadata().getProfilePointMap().entrySet().removeIf(next -> next.getKey().equals(id));
    }

    @Override
    public void upsertDevice(Device device) {
        // Add device to context
        driverContext.getDriverMetadata().getDeviceMap().put(device.getId(), device);
        // Add device driver info to context
        driverContext.getDriverMetadata().getDriverInfoMap().computeIfAbsent(device.getId(), k -> new ConcurrentHashMap<>(16));
        // Add device point info to context
        driverContext.getDriverMetadata().getPointInfoMap().computeIfAbsent(device.getId(), k -> new ConcurrentHashMap<>(16));
    }

    @Override
    public void deleteDevice(String id) {
        driverContext.getDriverMetadata().getDeviceMap().entrySet().removeIf(next -> next.getKey().equals(id));
        driverContext.getDriverMetadata().getDriverInfoMap().entrySet().removeIf(next -> next.getKey().equals(id));
        driverContext.getDriverMetadata().getPointInfoMap().entrySet().removeIf(next -> next.getKey().equals(id));
    }

    @Override
    public void upsertPoint(Point point) {
        // Upsert point to profile point map context
        driverContext.getDriverMetadata().getProfilePointMap().computeIfAbsent(point.getProfileId(), k -> new ConcurrentHashMap<>(16)).put(point.getId(), point);
    }

    @Override
    public void deletePoint(String profileId, String pointId) {
        // Delete point from profile point map context
        driverContext.getDriverMetadata().getProfilePointMap().computeIfPresent(profileId, (k, v) -> {
            v.entrySet().removeIf(next -> next.getKey().equals(pointId));
            return v;
        });
    }

    @Override
    public void upsertDriverInfo(DriverInfo driverInfo) {
        DriverAttribute attribute = driverContext.getDriverMetadata().getDriverAttributeMap().get(driverInfo.getDriverAttributeId());
        if (ObjectUtil.isNotNull(attribute)) {
            // Add driver info to driver info map context
            driverContext.getDriverMetadata().getDriverInfoMap().computeIfAbsent(driverInfo.getDeviceId(), k -> new ConcurrentHashMap<>(16))
                    .put(attribute.getAttributeName(), new AttributeInfo(driverInfo.getConfigValue(), attribute.getAttributeTypeFlag()));
        }
    }

    @Override
    public void deleteDriverInfo(String deviceId, String attributeId) {
        DriverAttribute attribute = driverContext.getDriverMetadata().getDriverAttributeMap().get(attributeId);
        if (ObjectUtil.isNotNull(attribute)) {
            // Delete driver info from driver info map context
            driverContext.getDriverMetadata().getDriverInfoMap().computeIfPresent(deviceId, (k, v) -> {
                v.entrySet().removeIf(next -> next.getKey().equals(attribute.getAttributeName()));
                return v;
            });

            // If the driver attribute is null, delete the driver info from the driver info map context
            driverContext.getDriverMetadata().getDriverInfoMap().entrySet().removeIf(next -> next.getValue().size() < 1);
        }
    }

    @Override
    public void upsertPointInfo(PointInfo pointInfo) {
        PointAttribute attribute = driverContext.getDriverMetadata().getPointAttributeMap().get(pointInfo.getPointAttributeId());
        if (ObjectUtil.isNotNull(attribute)) {
            // Add the point info to the device point info map context
            driverContext.getDriverMetadata().getPointInfoMap().computeIfAbsent(pointInfo.getDeviceId(), k -> new ConcurrentHashMap<>(16))
                    .computeIfAbsent(pointInfo.getPointId(), k -> new ConcurrentHashMap<>(16))
                    .put(attribute.getAttributeName(), new AttributeInfo(pointInfo.getConfigValue(), attribute.getAttributeTypeFlag()));
        }
    }

    @Override
    public void deletePointInfo(String deviceId, String pointId, String attributeId) {
        PointAttribute attribute = driverContext.getDriverMetadata().getPointAttributeMap().get(attributeId);
        if (ObjectUtil.isNotNull(attribute)) {
            // Delete the point info from the device info map context
            driverContext.getDriverMetadata().getPointInfoMap().computeIfPresent(deviceId, (key1, value1) -> {
                value1.computeIfPresent(pointId, (key2, value2) -> {
                    value2.entrySet().removeIf(next -> next.getKey().equals(attribute.getAttributeName()));
                    return value2;
                });
                return value1;
            });

            // If the point attribute is null, delete the point info from the point info map context
            driverContext.getDriverMetadata().getPointInfoMap().computeIfPresent(deviceId, (key, value) -> {
                value.entrySet().removeIf(next -> next.getValue().size() < 1);
                return value;
            });
        }
    }

    private void registerHandshake() {
        try {
            threadPoolExecutor.submit(() -> {
                driverService.driverEventSender(new DriverEvent(
                        serviceName,
                        EventConstant.Driver.HANDSHAKE,
                        null
                ));

                while (!StatusEnum.REGISTERING.equals(driverContext.getDriverStatus())) {
                    ThreadUtil.sleep(500);
                }
            }).get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // TODO 待推敲
            driverService.close("The driver initialization failed, Check whether dc3-center-manager are started normally");
            Thread.currentThread().interrupt();
        }
    }

    private void syncDriverMetadata(Driver driver) {
        try {
            threadPoolExecutor.submit(() -> {
                driverService.driverEventSender(new DriverEvent(
                        serviceName,
                        EventConstant.Driver.METADATA_SYNC,
                        driver.getServiceName()
                ));

                while (!StatusEnum.ONLINE.equals(driverContext.getDriverStatus())) {
                    ThreadUtil.sleep(500);
                }
            }).get(5, TimeUnit.MINUTES);
        } catch (Exception ignored) {
            // TODO 待推敲
            driverService.close("The driver initialization failed, Sync driver metadata from dc3-center-manager timeout");
            Thread.currentThread().interrupt();
        }
    }
}
