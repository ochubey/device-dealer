package io.ochubey.devices.repository;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.ochubey.appium.AppiumServicePool;
import io.ochubey.appium.android.AndroidAppiumServer;
import io.ochubey.appium.ios.IosAppiumServer;
import io.ochubey.devices.Device;
import io.ochubey.devices.DeviceDescriptor;
import io.ochubey.devices.status.DeviceStatuses;
import org.apache.commons.exec.OS;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static io.appium.java_client.remote.MobilePlatform.ANDROID;


public class DeviceUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceUpdater.class);

    private DeviceRepository repository;
    private final ReentrantLock client = new ReentrantLock();

    public DeviceUpdater(DeviceRepository repository) {
        this.repository = repository;
    }

    private void saveDevice(String udid, String deviceName, String platform, String platformVersion) {
        client.lock();
        LOG.info("Locked to work with device {}", deviceName);

        Device device = repository.findByUdid(udid);
        if (device == null || device.getDeviceStatus().equals(DeviceStatuses.DISCONNECTED)) {
            if (device != null && device.getDeviceStatus().equals(DeviceStatuses.DISCONNECTED)) {
                LOG.warn("Device was reconnected, so we trying to create new Appium session: {}:{}", device.getDeviceName(), device.getUdid());
                setStatus(device, DeviceStatuses.NEED_SETUP);
                getAppiumServerUrl(device);
            } else {
                int webPort = getFreePort();
                int driverPort = getFreePort();
                int serverPort = getFreePort();

                device = new Device(udid, deviceName, platform, platformVersion, webPort, driverPort, serverPort);
                if (driverPort != 0 && webPort != 0 && serverPort != 0) {
                    repository.save(device);
                    getAppiumServerUrl(device);
                } else {
                    LOG.error("Impossible to save device since there is no free port available.");
                    setStatus(device, DeviceStatuses.NEED_SETUP);
                    repository.save(device);
                }
            }
        }
        client.unlock();
        LOG.info("Unlocked after working with device {}", deviceName);
    }

    private int getFreePort() {
        int retry = 100;
        while (retry >= 0) {
            int port = getOpenPort();

            if (isWebPortNotBooked(port) && isDriverPortNotBooked(port) && isServerPortNotBooked(port)) {
                return port;
            } else {
                retry = retry - 1;
                String portAlreadyBookedMsg = "Port already booked for device. Retries remaining: " + retry;
                LOG.error(portAlreadyBookedMsg);
            }
        }
        return 0;
    }

    private boolean isWebPortNotBooked(int port) {
        Device device = repository.findByWebPort(port);
        if (device == null) {
            return true;
        } else {
            String webPortAlreadyTakenMsg = "Web Port already taken by driver with UDID " + device.getUdid();
            LOG.error(webPortAlreadyTakenMsg);
            return false;
        }
    }

    private boolean isDriverPortNotBooked(int port) {
        Device device = repository.findByDriverPort(port);
        if (device == null) {
            return true;
        } else {
            String driverPortAlreadyTakenMsg = "Driver Port already taken by driver with UDID " + device.getUdid();
            LOG.error(driverPortAlreadyTakenMsg);
            return false;
        }
    }

    private boolean isServerPortNotBooked(int port) {
        Device device = repository.findByServerPort(port);
        if (device == null) {
            return true;
        } else {
            String serverPortAlreadyTakenMsg = "Server Port already taken by driver with UDID " + device.getUdid();
            LOG.error(serverPortAlreadyTakenMsg);
            return false;
        }
    }

    public void saveDevice(DeviceDescriptor deviceDescriptor, String platform) {
        if (deviceDescriptor != null) {
            String deviceName = deviceDescriptor.getDeviceName();
            String platformVersion = deviceDescriptor.getPlatformVersion();
            String udid = deviceDescriptor.getUdid();

            saveDevice(udid, deviceName, platform, platformVersion);
        }
    }

    public void deleteInactiveDevice(String platform, List<String> activeDevices) {
        List<Device> devices = repository.findAllConnectedByPlatform(platform);
        if (activeDevices.isEmpty()) {
            for (Device device : devices) {
                disconnectDevice(device);
            }
        } else {
            for (Device device : devices) {
                if (!activeDevices.contains(device.getUdid())) {
                    disconnectDevice(device);
                }
            }
        }
    }

    public void setStatus(@NotNull Device device, String status) {
        device.setDeviceStatus(status);
        this.repository.save(device);
    }

    private void stopService(int serverPort) {
        for (AppiumDriverLocalService appiumDriverLocalService : AppiumServicePool.getServicePool().getServiceList()) {
            if (appiumDriverLocalService.getUrl().getPort() == serverPort) {
                String serviceFoundMsg = "Service found with port " + serverPort;
                LOG.info(serviceFoundMsg);
                appiumDriverLocalService.stop();
                String serverUpMsg = "Appium Server with port '" + serverPort + "' is up: " + appiumDriverLocalService.isRunning();
                LOG.info(serverUpMsg);
            }
        }
    }

    private static int getOpenPort() {
        int port = 0;
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            port = socket.getLocalPort();
        } catch (IOException e) {
            LOG.trace(e.getMessage(), e);
        }
        return port;
    }

    public static void cleanDeviceDataBaseOnStart(@NotNull DeviceRepository repository) {
        List<Device> deviceList = repository.findAll();
        for (Device device : deviceList) {
            killProcessByPort(device.getServerPort());
            killProcessByPort(device.getDriverPort());
            killProcessByPort(device.getWebPort());
        }
        repository.deleteAll();
    }

    private void disconnectDevice(@NotNull Device device) {
        killProcessByPort(device.getDriverPort());
        killProcessByPort(device.getWebPort());
        stopService(device.getServerPort());
        setStatus(device, DeviceStatuses.DISCONNECTED);
        LOG.warn("Device {}, {} was disconnected", device.getDeviceName(), device.getUdid());
    }

    private void getAppiumServerUrl(@NotNull Device device) {
        if (device.getPlatform().equalsIgnoreCase(ANDROID)) {
            AndroidAppiumServer.getServerUrl(repository, device);
        } else {
            IosAppiumServer.getServerUrl(repository, device);
        }
    }

    public static void killProcessByPort(int port) {
        if (OS.isFamilyMac()) {
            String[] commands = new String[]{
                    String.format("lsof -i tcp:%s", Integer.toString(port)),
                    "| grep node",
                    "| grep -v PID",
                    "| grep LISTEN",
                    "| grep -v grep",
                    "| awk '{print $2}'",
                    "| xargs kill;"
            };

            String command = String.join(" ", commands);

            Process p;
            try {
                p = Runtime.getRuntime().exec(command);
                p.waitFor();
            } catch (IOException | InterruptedException e) {
                LOG.trace(e.getMessage(), e);
            }
        } else if (OS.isFamilyWindows()) {
            //TODO: implement process killing for Windows based OS
        } else if (OS.isFamilyUnix()) {
            //TODO: implement process killing for Unix based OS
        } else {
            LOG.error("Unable to stop process by port number: {} on your operating system", port);
        }
    }
}
