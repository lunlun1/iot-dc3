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

package io.github.pnoker.common.sdk.service.rabbit;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import com.rabbitmq.client.Channel;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.driver.EventConstant;
import io.github.pnoker.common.constant.driver.MetadataConstant;
import io.github.pnoker.common.entity.driver.DriverConfiguration;
import io.github.pnoker.common.entity.driver.DriverMetadata;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.enums.StatusEnum;
import io.github.pnoker.common.model.*;
import io.github.pnoker.common.sdk.bean.driver.DriverContext;
import io.github.pnoker.common.sdk.service.DriverMetadataService;
import io.github.pnoker.common.sdk.service.DriverService;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 接收驱动发送过来的数据
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverMetadataReceiver {

    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;
    @Resource
    private DriverMetadataService driverMetadataService;

    @RabbitHandler
    @RabbitListener(queues = "#{driverMetadataQueue.name}")
    public void driverConfigurationReceive(Channel channel, Message message, DriverConfiguration driverConfiguration) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            if (null == driverConfiguration || CharSequenceUtil.isEmpty(driverConfiguration.getType()) || CharSequenceUtil.isEmpty(driverConfiguration.getCommand())) {
                log.error("Invalid driver configuration {}", driverConfiguration);
                return;
            }

            switch (driverConfiguration.getType()) {
                case PrefixConstant.DRIVER:
                    configurationDriver(driverConfiguration);
                    break;
                case PrefixConstant.PROFILE:
                    configurationProfile(driverConfiguration);
                    break;
                case PrefixConstant.DEVICE:
                    configurationDevice(driverConfiguration);
                    break;
                case PrefixConstant.POINT:
                    configurationPoint(driverConfiguration);
                    break;
                case PrefixConstant.DRIVER_INFO:
                    configurationDriverInfo(driverConfiguration);
                    break;
                case PrefixConstant.POINT_INFO:
                    configurationPointInfo(driverConfiguration);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 配置 driver
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationDriver(DriverConfiguration driverConfiguration) {
        if (!ResponseEnum.OK.equals(driverConfiguration.getResponse())) {
            driverService.close("The driver initialization failed: {}", driverConfiguration.getResponse());
        }

        switch (driverConfiguration.getCommand()) {
            case EventConstant.Driver.HANDSHAKE_BACK:
                driverContext.setDriverStatus(StatusEnum.REGISTERING);
                break;
            case EventConstant.Driver.REGISTER_BACK:
                driverContext.setDriverStatus(StatusEnum.ONLINE);
                break;
            case EventConstant.Driver.METADATA_SYNC_BACK:
                String s = JsonUtil.toPrettyJsonString(driverConfiguration.getContent());
                log.info(s);
                DriverMetadata driverMetadata = JsonUtil.parseObject(s, DriverMetadata.class);
                driverContext.setDriverMetadata(driverMetadata);
                driverMetadata.getDriverAttributeMap().values().forEach(driverAttribute -> log.info("Syncing driver attribute[{}] metadata: {}", driverAttribute.getDisplayName(), JsonUtil.toJsonString(driverAttribute)));
                driverMetadata.getPointAttributeMap().values().forEach(pointAttribute -> log.info("Syncing point attribute[{}] metadata: {}", pointAttribute.getDisplayName(), JsonUtil.toJsonString(pointAttribute)));
                driverMetadata.getDeviceMap().values().forEach(device -> log.info("Syncing device[{}] metadata: {}", device.getDeviceName(), JsonUtil.toJsonString(device)));
                log.info("The metadata synced successfully");
                break;
            default:
                break;
        }
    }

    /**
     * 配置 driver profile
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationProfile(DriverConfiguration driverConfiguration) {
        Profile profile = Convert.convert(Profile.class, driverConfiguration.getContent());
        if (MetadataConstant.Profile.ADD.equals(driverConfiguration.getCommand()) || MetadataConstant.Profile.UPDATE.equals(driverConfiguration.getCommand())) {
            log.info("Upsert profile \n{}", JsonUtil.toJsonString(profile));
            driverMetadataService.upsertProfile(profile);
        } else if (MetadataConstant.Profile.DELETE.equals(driverConfiguration.getCommand())) {
            log.info("Delete profile {}", profile.getProfileName());
            driverMetadataService.deleteProfile(profile.getId());
        }
    }

    /**
     * 配置 driver device
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationDevice(DriverConfiguration driverConfiguration) {
        Device device = Convert.convert(Device.class, driverConfiguration.getContent());
        if (MetadataConstant.Device.ADD.equals(driverConfiguration.getCommand()) || MetadataConstant.Device.UPDATE.equals(driverConfiguration.getCommand())) {
            log.info("Upsert device \n{}", JsonUtil.toJsonString(device));
            driverMetadataService.upsertDevice(device);
        } else if (MetadataConstant.Device.DELETE.equals(driverConfiguration.getCommand())) {
            log.info("Delete device {}", device.getDeviceName());
            driverMetadataService.deleteDevice(device.getId());
        }
    }

    /**
     * 配置 driver point
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationPoint(DriverConfiguration driverConfiguration) {
        Point point = Convert.convert(Point.class, driverConfiguration.getContent());
        if (MetadataConstant.Point.ADD.equals(driverConfiguration.getCommand()) || MetadataConstant.Point.UPDATE.equals(driverConfiguration.getCommand())) {
            log.info("Upsert point \n{}", JsonUtil.toJsonString(point));
            driverMetadataService.upsertPoint(point);
        } else if (MetadataConstant.Point.DELETE.equals(driverConfiguration.getCommand())) {
            log.info("Delete point {}", point.getPointName());
            driverMetadataService.deletePoint(point.getProfileId(), point.getId());
        }
    }

    /**
     * 配置 driver info
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationDriverInfo(DriverConfiguration driverConfiguration) {
        DriverInfo driverInfo = Convert.convert(DriverInfo.class, driverConfiguration.getContent());
        if (MetadataConstant.DriverInfo.ADD.equals(driverConfiguration.getCommand()) || MetadataConstant.DriverInfo.UPDATE.equals(driverConfiguration.getCommand())) {
            log.info("Upsert driver info \n{}", JsonUtil.toJsonString(driverInfo));
            driverMetadataService.upsertDriverInfo(driverInfo);
        } else if (MetadataConstant.DriverInfo.DELETE.equals(driverConfiguration.getCommand())) {
            log.info("Delete driver info {}", driverInfo);
            driverMetadataService.deleteDriverInfo(driverInfo.getDeviceId(), driverInfo.getDriverAttributeId());
        }
    }

    /**
     * 配置 driver point info
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationPointInfo(DriverConfiguration driverConfiguration) {
        PointInfo pointInfo = Convert.convert(PointInfo.class, driverConfiguration.getContent());
        if (MetadataConstant.PointInfo.ADD.equals(driverConfiguration.getCommand()) || MetadataConstant.PointInfo.UPDATE.equals(driverConfiguration.getCommand())) {
            log.info("Upsert point info \n{}", JsonUtil.toJsonString(pointInfo));
            driverMetadataService.upsertPointInfo(pointInfo);
        } else if (MetadataConstant.PointInfo.DELETE.equals(driverConfiguration.getCommand())) {
            log.info("Delete point info {}", pointInfo);
            driverMetadataService.deletePointInfo(pointInfo.getPointId(), pointInfo.getId(), pointInfo.getPointAttributeId());
        }
    }

}
