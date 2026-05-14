package tech.beawitch.rpc.compressor.impl;

import tech.beawitch.rpc.compressor.Compressor;
import tech.beawitch.rpc.exception.RpcException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipCompressor implements Compressor {


    @Override
    public byte[] compress(byte[] data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(data);
            gzip.finish();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RpcException("压缩失败", e);
        }
    }

    @Override
    public byte[] decompress(byte[] data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] buffer = new byte[4069];
            int len;
            while ((len = gzip.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new RpcException("解压缩失败", e);
        }
    }
}
