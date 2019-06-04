package io.ochubey.devices.ios;

import io.ochubey.appium.DeviceWaitHelper;
import io.ochubey.devices.DeviceDescriptor;
import io.ochubey.devices.repository.DeviceRepository;
import io.ochubey.devices.repository.DeviceUpdater;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static io.appium.java_client.remote.MobilePlatform.IOS;


public class IphoneLocator implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(IphoneLocator.class);
    private static final String IPHONE_DEVICE_LOCATOR_COMMAND = "instruments -s devices";
    private DeviceUpdater deviceUpdater;

    public IphoneLocator(DeviceRepository repository) {
        LOG.warn("iPhone device locator started");
        deviceUpdater = new DeviceUpdater(repository);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        try {
            while (true) {
                checkForDeviceStatus();
            }
        } catch (IllegalStateException ex) {
            LOG.error("Looks like server execution was interrupted.");
        }
    }

    private void checkForDeviceStatus() {
        List<String> currentDevices = saveActiveDevices();
        deviceUpdater.deleteInactiveDevice(IOS, currentDevices);
        DeviceWaitHelper.doSleep();
    }

    private List<String> saveActiveDevices() {
        List<String> currentDevices = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(IPHONE_DEVICE_LOCATOR_COMMAND);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String errorLine;
            while ((errorLine = stdError.readLine()) != null) {
                LOG.error(errorLine);
            }
            String deviceString;
            while ((deviceString = stdInput.readLine()) != null) {
                if (deviceString.contains("[") && !deviceString.contains("Apple Watch") && !deviceString.contains("-") && !deviceString.contains("(Simulator)")) {
                    DeviceDescriptor deviceDescriptor = extractDeviceDescriptor(deviceString);
                    deviceUpdater.saveDevice(deviceDescriptor, IOS.toLowerCase());
                    if (deviceDescriptor != null) {
                        currentDevices.add(deviceDescriptor.getUdid());
                    }
                }
            }
        } catch (Exception e) {
            LOG.trace(e.getMessage(), e);
        }
        return currentDevices;
    }

    @Nullable
    private DeviceDescriptor extractDeviceDescriptor(String deviceString) {
        String deviceName;

        String[] nameExtracted = deviceString.split(" \\(");
        if (nameExtracted.length == 2) {
            deviceName = nameExtracted[0];
        } else {
            return null;
        }

        String platformVersion;
        String[] platformExtracted = nameExtracted[1].split("\\) \\[");
        if (platformExtracted.length == 2) {
            platformVersion = platformExtracted[0];
        } else {
            return null;
        }

        String udid = platformExtracted[1].replace("]", "");

        return new DeviceDescriptor(deviceName, platformVersion, udid);
    }
}
