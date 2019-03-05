package io.ochubey.devices;

public class DeviceDescriptor {

    private String deviceName;
    private String platformVersion;
    private String udid;

    public DeviceDescriptor(String deviceName, String platformVersion, String udid) {
        this.deviceName = deviceName;
        this.platformVersion = platformVersion;
        this.udid = udid;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public String getUdid() {
        return udid;
    }
}
