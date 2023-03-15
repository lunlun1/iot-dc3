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

package io.github.pnoker.common.sdk.bean.driver;

import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.model.DriverAttribute;
import io.github.pnoker.common.model.PointAttribute;
import io.github.pnoker.common.sdk.bean.schedule.ScheduleProperty;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * 驱动配置文件 driver 字段内容
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@Validated
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "driver")
public class DriverProperty {
    /**
     * 驱动名称
     */
    @NotBlank(message = "driver name can't be empty")
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "Invalid driver name")
    private String name;

    /**
     * 驱动类型
     */
    @NotNull(message = "driver type can't be empty")
    private DriverTypeFlagEnum type = DriverTypeFlagEnum.DRIVER;

    /**
     * 租户
     */
    @NotBlank(message = "driver tenant can't be empty")
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "Invalid driver tenant")
    private String tenant;

    /**
     * 描述
     */
    private String remark;

    /**
     * 定时任务相关属性
     */
    private ScheduleProperty schedule;

    /**
     * 驱动属性
     */
    private List<DriverAttribute> driverAttribute;

    /**
     * 位号属性
     */
    private List<PointAttribute> pointAttribute;
}
