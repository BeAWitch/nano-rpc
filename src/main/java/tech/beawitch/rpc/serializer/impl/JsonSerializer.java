package tech.beawitch.rpc.serializer.impl;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import tech.beawitch.rpc.serializer.Serializer;

import java.nio.charset.StandardCharsets;

public class JsonSerializer implements Serializer {

    @Override
    public byte[] serialize(Object obj) {
        return JSONObject.toJSONString(obj).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        String jsonString = new String(bytes, StandardCharsets.UTF_8);
        return JSONObject.parseObject(jsonString, clazz, JSONReader.Feature.SupportClassForName);
    }
}
