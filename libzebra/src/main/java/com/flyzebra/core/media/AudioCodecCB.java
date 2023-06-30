package com.flyzebra.core.media;

public interface AudioCodecCB {

    void notifyAacHead(int channel, byte[] head, int size);

    void notifyAacData(int channel, byte[] data, int size, long pts);
}
