package tech.beawitch.rpc.serializer.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import lombok.extern.slf4j.Slf4j;
import tech.beawitch.rpc.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
public class HessianSerializer implements Serializer {

    @Override
    public byte[] serialize(Object obj) {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Hessian2Output output = new Hessian2Output(out);
            output.writeObject(obj);
            output.flush();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Hessian 序列化失败 {}", obj.getClass().getName(), e);
            return new byte[0];
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try(ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            Hessian2Input input = new Hessian2Input(in);
            return (T) input.readObject();
        } catch (Exception e) {
            log.error("Hessian 反序列化失败 {}", clazz.getName(), e);
            return null;
        }
    }
}
