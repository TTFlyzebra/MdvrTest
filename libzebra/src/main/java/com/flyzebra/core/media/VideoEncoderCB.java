package com.flyzebra.core.media;

public interface VideoEncoderCB {
    /**
     * H264 sps-pps
     *
     * @param data
     * @param size
     */
    void notifySpsPps(int channel, byte[] data, int size);

    /**
     * H265 vps-sps-pps
     *
     * @param data
     * @param size
     */
    void notifyVpsSpsPps(int channel, byte[] data, int size);

    /**
     * h264
     *
     * @param data
     * @param size
     * @param pts
     */
    void notifyAvcData(int channel, byte[] data, int size, long pts);

    /**
     * h265
     *
     * @param data
     * @param size
     * @param pts
     */
    void notifyHevcData(int channel, byte[] data, int size, long pts);
}
