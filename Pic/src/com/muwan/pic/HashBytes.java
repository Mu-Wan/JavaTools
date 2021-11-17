package com.muwan.pic;

import java.util.Arrays;

/**
 * @author MuWan
 */
public class HashBytes {
    public byte[] getHashBytes() {
        return hashBytes;
    }

    public void setHashBytes(byte[] hashBytes) {
        this.hashBytes = hashBytes;
    }

    private byte[] hashBytes;
    public HashBytes(byte[] bytes) {
        this.hashBytes = bytes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HashBytes) {
            return Arrays.equals(((HashBytes)obj).getHashBytes(), this.hashBytes);
        } else {
            return false;
        }
    }
}
