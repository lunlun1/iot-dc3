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

package io.github.pnoker.common.sdk.utils;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.exception.OutRangeException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.utils.ArithmeticUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 类型转换相关工具类集合
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
public class ConvertUtil {

    private ConvertUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 位号数据处理
     * 当出现精度问题，向上调整
     * 例如：byte 类型的数据经过 base 和 multiple 之后超出范围，将其调整为float类型
     *
     * @param point    Point
     * @param rawValue Raw Value
     * @return Value
     */
    public static String convertValue(Point point, String rawValue) {
        PointTypeFlagEnum valueType = point.getPointTypeFlag();
        if (ObjectUtil.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of {}", point.getPointTypeFlag());
        }

        BigDecimal base = ObjectUtil.isNotNull(point.getBaseValue()) ? point.getBaseValue() : new BigDecimal(0);
        BigDecimal multiple = ObjectUtil.isNotNull(point.getMultiple()) ? point.getMultiple() : new BigDecimal(1);
        int decimal = ObjectUtil.isNotNull(point.getValueDecimal()) ? point.getValueDecimal() : 6;

        Object value;
        switch (valueType) {
            case BYTE:
                value = convertByte(rawValue, base, multiple);
                break;
            case SHORT:
                value = convertShort(rawValue, base, multiple);
                break;
            case INT:
                value = convertInteger(rawValue, base, multiple);
                break;
            case LONG:
                value = convertLong(rawValue, base, multiple);
                break;
            case FLOAT:
                value = convertFloat(rawValue, base, multiple, decimal);
                break;
            case DOUBLE:
                value = convertDouble(rawValue, base, multiple, decimal);
                break;
            case BOOLEAN:
                value = convertBoolean(rawValue);
                break;
            default:
                value = rawValue;
                break;
        }

        return String.valueOf(value);
    }

    /**
     * 字符串转短字节值
     * -128 ~ 127
     *
     * @param content 字符串
     * @return short
     */
    private static byte convertByte(String content, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = linear(multiple, content, base);
            return multiply.byteValue();
        } catch (Exception e) {
            throw new OutRangeException("Out of byte range: {} ~ {}, current: {}", Byte.MIN_VALUE, Byte.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转短整数值
     * -32768 ~ 32767
     *
     * @param content 字符串
     * @return short
     */
    private static short convertShort(String content, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = linear(multiple, content, base);
            return multiply.shortValue();
        } catch (Exception e) {
            throw new OutRangeException("Out of short range: {} ~ {}, current: {}", Short.MIN_VALUE, Short.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转整数值
     * -2147483648 ~ 2147483647
     *
     * @param content 字符串
     * @return int
     */
    private static int convertInteger(String content, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = linear(multiple, content, base);
            return multiply.intValue();
        } catch (Exception e) {
            throw new OutRangeException("Out of int range: {} ~ {}, current: {}", Integer.MIN_VALUE, Integer.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转长整数值
     * -9223372036854775808 ~ 9223372036854775807
     *
     * @param content 字符串
     * @return long
     */
    private static long convertLong(String content, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = linear(multiple, content, base);
            return multiply.longValue();
        } catch (Exception e) {
            throw new OutRangeException("Out of long range: {} ~ {}, current: {}", Long.MIN_VALUE, Long.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转浮点值
     *
     * @param content 字符串
     * @return float
     */
    private static float convertFloat(String content, BigDecimal base, BigDecimal multiple, int decimal) {
        try {
            BigDecimal multiply = linear(multiple, content, base);
            if (Float.isInfinite(multiply.floatValue())) {
                throw new OutRangeException();
            }
            return ArithmeticUtil.round(multiply.floatValue(), decimal);
        } catch (Exception e) {
            throw new OutRangeException("Out of float range: |{} ~ {}|, current: {}", Float.MIN_VALUE, Float.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转双精度浮点值
     *
     * @param content 字符串
     * @return double
     */
    private static double convertDouble(String content, BigDecimal base, BigDecimal multiple, int decimal) {
        try {
            BigDecimal multiply = linear(multiple, content, base);
            if (Double.isInfinite(multiply.doubleValue())) {
                throw new OutRangeException();
            }
            return ArithmeticUtil.round(multiply.doubleValue(), decimal);
        } catch (Exception e) {
            throw new OutRangeException("Out of double range: |{} ~ {}|, current: {}", Double.MIN_VALUE, Double.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转布尔值
     *
     * @param content 字符串
     * @return boolean
     */
    private static boolean convertBoolean(String content) {
        return Boolean.parseBoolean(content);
    }

    /**
     * 线性函数：y = ax + b
     *
     * @param x A
     * @param b B
     * @param a X
     * @return BigDecimal
     */
    private static BigDecimal linear(BigDecimal a, String x, BigDecimal b) {
        BigDecimal multiply = a.multiply(new BigDecimal(x));
        return multiply.add(b);
    }
}
