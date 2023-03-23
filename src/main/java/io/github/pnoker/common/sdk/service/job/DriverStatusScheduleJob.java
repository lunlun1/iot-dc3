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

package io.github.pnoker.common.sdk.service.job;

import io.github.pnoker.common.dto.DriverEventDTO;
import io.github.pnoker.common.dto.DriverStatusDTO;
import io.github.pnoker.common.enums.DriverEventTypeEnum;
import io.github.pnoker.common.sdk.bean.DriverContext;
import io.github.pnoker.common.sdk.service.DriverService;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 自定义调度任务
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverStatusScheduleJob extends QuartzJobBean {

    @Value("${spring.application.name}")
    private String serviceName;
    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        DriverStatusDTO driverStatusDTO = new DriverStatusDTO(serviceName, driverContext.getDriverStatus());
        DriverEventDTO driverEventDTO = new DriverEventDTO(DriverEventTypeEnum.HEARTBEAT, JsonUtil.toJsonString(driverStatusDTO));
        driverService.driverEventSender(driverEventDTO);
    }
}