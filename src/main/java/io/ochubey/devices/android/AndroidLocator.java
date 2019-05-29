package io.ochubey.devices.android;

import io.ochubey.appium.DeviceWaiter;
import io.ochubey.devices.DeviceDescriptor;
import io.ochubey.devices.repository.DeviceRepository;
import io.ochubey.devices.repository.DeviceUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static io.appium.java_client.remote.MobilePlatform.ANDROID;
import static io.ochubey.utils.ConfigurationValidator.isShouldUseEmulators;

public class AndroidLocator implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AndroidLocator.class);
    private static final String ANDROID_DEVICE_LOCATOR_COMMAND = "adb devices -l";
    private String adbShellPattern = "adb -s %s shell %s";
    private DeviceUpdater deviceUpdater;

    public AndroidLocator(DeviceRepository repository) {
        LOG.warn("Android device locator started");
        deviceUpdater = new DeviceUpdater(repository);
    }

    @Override
    public void run() {
        do {
            checkFroDeviceStatus();
        } while (true);
    }

    private void checkFroDeviceStatus() {
        List<String> currentDevices = saveActiveAndroidDevices();
        deviceUpdater.deleteInactiveDevice(ANDROID, currentDevices);
        DeviceWaiter.doSleep();
    }

    private List<String> saveActiveAndroidDevices() {
        List<String> currentDevices = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(ANDROID_DEVICE_LOCATOR_COMMAND);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String deviceString;
            while ((deviceString = stdInput.readLine()) != null) {
                if (deviceString.contains("model:") && deviceString.contains("device:") && shouldIncludeIfEmulator(deviceString)) {
                    DeviceDescriptor deviceDescriptor = extractDeviceDescriptor(deviceString);
                    deviceUpdater.saveDevice(deviceDescriptor, ANDROID.toLowerCase());
                    if (deviceDescriptor != null) {
                        currentDevices.add(deviceDescriptor.getUdid());
                    }
                }
            }
        } catch (IOException e) {
            LOG.trace(e.getMessage(), e);
        }
        return currentDevices;
    }

    private boolean shouldIncludeIfEmulator(String deviceString) {
        if (!isShouldUseEmulators()) {
            return deviceString.contains(" usb:");
        } else {
            return true;
        }
    }

    @Nullable
    private DeviceDescriptor extractDeviceDescriptor(@NotNull String deviceString) {
        String udid = deviceString.split(" ")[0];
        String deviceName = getNameByUdid(udid);
        String platformVersion = getPlatformVersionByUdid(udid);
        if (deviceName != null && platformVersion != null) {
            return new DeviceDescriptor(deviceName, platformVersion, udid);
        }
        return null;
    }

    private String getPlatformVersionByUdid(String udid) {
        String adbVersionCommand = "getprop ro.build.version.release";
        String platformVersion = null;
        try {
            String command = String.format(adbShellPattern, udid, adbVersionCommand);
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            platformVersion = stdInput.readLine();
        } catch (IOException e) {
            LOG.trace(e.getMessage(), e);
        }
        return platformVersion;
    }

    private String getNameByUdid(String udid) {
        String emulatorUdidPrefix = "emulator";
        String adbModelCommand = "getprop ro.product.model";
        String adbEmulatorNamePattern = "adb -s %s emu avd name";

        String deviceName = null;
        String command;
        if (udid.contains(emulatorUdidPrefix)) {
            command = String.format(adbEmulatorNamePattern, udid);
        } else {
            command = String.format(adbShellPattern, udid, adbModelCommand);
        }

        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            deviceName = stdInput.readLine();
        } catch (IOException e) {
            LOG.trace(e.getMessage(), e);
        }
        return deviceName;
    }
}
