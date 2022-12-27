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

package io.github.pnoker.common.sdk.bean.driver;

import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.entity.DriverAttribute;
import io.github.pnoker.common.entity.PointAttribute;
import io.github.pnoker.common.sdk.bean.schedule.ScheduleProperty;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * 驱动配置文件 driver 字段内容
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Setter
@Getter
@Validated({Insert.class, Update.class})
@ConfigurationProperties(prefix = "driver")
public class DriverProperty {
    @NotBlank(message = "name can't be empty")
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "Invalid name,contains invalid characters or length is not in the range of 2~32",
            groups = {Insert.class, Update.class})
    private String tenant;
    private String name;
    private String type = PrefixConstant.DRIVER;
    private String remark;
    private ScheduleProperty schedule;
    private List<DriverAttribute> driverAttribute;
    private List<PointAttribute> pointAttribute;
}
