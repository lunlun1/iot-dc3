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

package io.github.pnoker.common.sdk.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.entity.driver.AttributeInfo;
import io.github.pnoker.common.model.*;
import io.github.pnoker.common.sdk.bean.DriverContext;
import io.github.pnoker.common.sdk.service.DriverMetadataTempService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Driver Metadata Service Implements
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverMetadataTempServiceImpl implements DriverMetadataTempService {

    @Resource
    private DriverContext driverContext;

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

}
