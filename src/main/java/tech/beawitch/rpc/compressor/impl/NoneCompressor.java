package tech.beawitch.rpc.compressor.impl;

import tech.beawitch.rpc.compressor.Compressor;

public class NoneCompressor implements Compressor {

    @Override
    public byte[] compress(byte[] data) {
        return data;
    }

    @Override
    public byte[] decompress(byte[] data) {
        return data;
    }
}
