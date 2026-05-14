package tech.beawitch.rpc.compressor;

import tech.beawitch.rpc.compressor.impl.GzipCompressor;
import tech.beawitch.rpc.compressor.impl.NoneCompressor;

import java.util.HashMap;
import java.util.Map;

public class CompressorManager {

    private final Map<Byte, Compressor> compressorMap = new HashMap<>();

    public CompressorManager() {
        init();
    }

    public Compressor getCompressor(byte code) {
        return compressorMap.get(code);
    }

    private void init() {
        compressorMap.put(Compressor.CompressorAlgorithm.NONE.getCode(), new NoneCompressor());
        compressorMap.put(Compressor.CompressorAlgorithm.GZIP.getCode(), new GzipCompressor());
    }
}
