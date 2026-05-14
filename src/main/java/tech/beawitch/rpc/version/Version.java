package tech.beawitch.rpc.version;

import lombok.Getter;

@Getter
public enum Version {
    V1(0);

    private final byte versionNo;

    Version(int versionNo) {
        this.versionNo = (byte) versionNo;
    }
}
