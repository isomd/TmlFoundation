package io.github.timemachinelab.util;

import com.alibaba.fastjson2.JSON;

public class JsonUtil {
    public static String toJson(Object object) {
        return JSON.toJSONString(object);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }
}
