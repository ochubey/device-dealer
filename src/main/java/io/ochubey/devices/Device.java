package io.ochubey.devices;

import org.springframework.data.annotation.Id;

import static io.ochubey.devices.status.DeviceStatuses.NEED_SETUP;
import static java.lang.String.format;

public class Device {

    @Id
    private String id;

    private String udid;
    private String deviceName;
    private String platform;
    private String platformVersion;

    private int serverPort;
    private int webPort;
    private int driverPort;

    private String deviceStatus;

    public Device(String udid, String deviceName, String platform, String platformVersion,
                  int webPort, int driverPort, int serverPort) {
        this.setUdid(udid);
        this.setDeviceName(deviceName);
        this.setPlatformVersion(platformVersion);
        this.setPlatform(platform);
        this.setWebPort(webPort);
        this.setDriverPort(driverPort);
        this.setDeviceStatus(NEED_SETUP);
        this.setServerPort(serverPort);
    }

    public String getUdid() {
        return udid;
    }

    public void setUdid(String udid) {
        this.udid = udid;
    }

    public String getDeviceName() {
        return deviceName;
    }

    private void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPlatform() {
        return platform;
    }

    private void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    private void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    public int getWebPort() {
        return webPort;
    }

    private void setWebPort(int webPort) {
        this.webPort = webPort;
    }

    public int getDriverPort() {
        return driverPort;
    }

    private void setDriverPort(int driverPort) {
        this.driverPort = driverPort;
    }

    public String getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public int getServerPort() {
        return serverPort;
    }

    private void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public String toString() {
        return format("Customer[id=%s, udid='%s', deviceName='%s',platformVersion='%s', platform='%s', " +
                        "deviceStatus='%s', Web Port='%s', Driver Port='%s', Server Port='%s']",
                id, getUdid(), getDeviceName(), getPlatformVersion(), getPlatform(),
                getDeviceStatus(), getWebPort(), getDriverPort(), getServerPort());
    }
}
