package com.flyzebra.media;

public interface AudioEncoderCB {

    void notifyAacHead(int channel, byte[] head, int size);

    void notifyAacData(int channel, byte[] data, int size, long pts);
}
