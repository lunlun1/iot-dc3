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

import io.github.pnoker.common.dto.DriverMetadataDTO;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.model.*;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.driver.sdk.service.DriverMetadataService;
import io.github.pnoker.driver.sdk.service.DriverMetadataTempService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 驱动元数据相关接口实现
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverMetadataServiceImpl implements DriverMetadataService {

    @Resource
    private DriverMetadataTempService driverMetadataTempService;

    @Override
    public void profileMetadata(DriverMetadataDTO entityDTO) {
        Profile profile = JsonUtil.parseObject(entityDTO.getContent(), Profile.class);
        if (MetadataCommandTypeEnum.ADD.equals(entityDTO.getMetadataCommandType()) || MetadataCommandTypeEnum.UPDATE.equals(entityDTO.getMetadataCommandType())) {
            log.info("Upsert profile: {}", JsonUtil.toJsonString(profile));
            driverMetadataTempService.upsertProfile(profile);
        } else if (MetadataCommandTypeEnum.DELETE.equals(entityDTO.getMetadataCommandType())) {
            log.info("Delete profile: {}", JsonUtil.toJsonString(profile));
            driverMetadataTempService.deleteProfile(profile.getId());
        }
    }

    @Override
    public void deviceMetadata(DriverMetadataDTO entityDTO) {
        Device device = JsonUtil.parseObject(entityDTO.getContent(), Device.class);
        if (MetadataCommandTypeEnum.ADD.equals(entityDTO.getMetadataCommandType()) || MetadataCommandTypeEnum.UPDATE.equals(entityDTO.getMetadataCommandType())) {
            log.info("Upsert device: {}", JsonUtil.toJsonString(device));
            driverMetadataTempService.upsertDevice(device);
        } else if (MetadataCommandTypeEnum.DELETE.equals(entityDTO.getMetadataCommandType())) {
            log.info("Delete device: {}", JsonUtil.toJsonString(device));
            driverMetadataTempService.deleteDevice(device.getId());
        }
    }

    @Override
    public void pointMetadata(DriverMetadataDTO entityDTO) {
        Point point = JsonUtil.parseObject(entityDTO.getContent(), Point.class);
        if (MetadataCommandTypeEnum.ADD.equals(entityDTO.getMetadataCommandType()) || MetadataCommandTypeEnum.UPDATE.equals(entityDTO.getMetadataCommandType())) {
            log.info("Upsert point: {}", JsonUtil.toJsonString(point));
            driverMetadataTempService.upsertPoint(point);
        } else if (MetadataCommandTypeEnum.DELETE.equals(entityDTO.getMetadataCommandType())) {
            log.info("Delete point: {}", JsonUtil.toJsonString(point));
            driverMetadataTempService.deletePoint(point.getProfileId(), point.getId());
        }
    }

    @Override
    public void driverInfoMetadata(DriverMetadataDTO entityDTO) {
        DriverInfo driverInfo = JsonUtil.parseObject(entityDTO.getContent(), DriverInfo.class);
        if (MetadataCommandTypeEnum.ADD.equals(entityDTO.getMetadataCommandType()) || MetadataCommandTypeEnum.UPDATE.equals(entityDTO.getMetadataCommandType())) {
            log.info("Upsert driver info: {}", JsonUtil.toJsonString(driverInfo));
            driverMetadataTempService.upsertDriverInfo(driverInfo);
        } else if (MetadataCommandTypeEnum.DELETE.equals(entityDTO.getMetadataCommandType())) {
            log.info("Delete driver info: {}", JsonUtil.toJsonString(driverInfo));
            driverMetadataTempService.deleteDriverInfo(driverInfo.getDeviceId(), driverInfo.getDriverAttributeId());
        }
    }

    @Override
    public void pointInfoMetadata(DriverMetadataDTO entityDTO) {
        PointInfo pointInfo = JsonUtil.parseObject(entityDTO.getContent(), PointInfo.class);
        if (MetadataCommandTypeEnum.ADD.equals(entityDTO.getMetadataCommandType()) || MetadataCommandTypeEnum.UPDATE.equals(entityDTO.getMetadataCommandType())) {
            log.info("Upsert point info: {}", JsonUtil.toJsonString(pointInfo));
            driverMetadataTempService.upsertPointInfo(pointInfo);
        } else if (MetadataCommandTypeEnum.DELETE.equals(entityDTO.getMetadataCommandType())) {
            log.info("Delete point info: {}", JsonUtil.toJsonString(pointInfo));
            driverMetadataTempService.deletePointInfo(pointInfo.getPointId(), pointInfo.getId(), pointInfo.getPointAttributeId());
        }
    }
}
