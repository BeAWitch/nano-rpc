package tech.beawitch.rpc.compressor;

import lombok.Getter;

public interface Compressor {

    byte[] compress(byte[] data);

    byte[] decompress(byte[] data);

    @Getter
    enum CompressorAlgorithm {
        NONE((byte) 0),
        GZIP((byte) 1);

        private final byte code;

        CompressorAlgorithm(byte code) {
            this.code = code;
        }
    }
}
