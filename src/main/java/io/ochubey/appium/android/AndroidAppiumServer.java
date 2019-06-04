package io.ochubey.appium.android;

import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.AndroidServerFlag;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.ochubey.appium.AppiumLogOutputStream;
import io.ochubey.appium.ios.WdaServer;
import io.ochubey.devices.Device;
import io.ochubey.devices.repository.DeviceRepository;
import io.ochubey.devices.repository.DeviceUpdater;
import io.ochubey.devices.status.DeviceStatusHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import static io.appium.java_client.service.local.flags.GeneralServerFlag.SESSION_OVERRIDE;
import static io.ochubey.appium.AppiumServicePool.getServicePool;
import static io.ochubey.appium.ios.IosAppiumServer.IP_ADDRESS;
import static io.ochubey.devices.status.DeviceStatusHelper.IDLE;
import static io.ochubey.devices.status.DeviceStatusHelper.NEED_SETUP;
import static io.ochubey.utils.ConfigurationValidator.getBlackListBundleIds;

/**
 * Created by ochubey on 1/4/18.
 */
public class AndroidAppiumServer {

    private static final Logger LOG = LoggerFactory.getLogger(AndroidAppiumServer.class);
    private static String adbShellPrefix = "adb -s %s shell %s";

    private AndroidAppiumServer() {
        throw new IllegalStateException("Utility class");
    }

    @Nullable
    public static URL getServerUrl(DeviceRepository repository, @NotNull Device device) {
        URL statusUrl = null;
        URL serverUrl = null;
        String serverStringUrl = String.format("http://%s:%s/wd/hub", IP_ADDRESS, device.getServerPort());
        try {
            statusUrl = new URL(serverStringUrl + "/status");
            serverUrl = new URL(serverStringUrl);
        } catch (MalformedURLException e) {
            LOG.trace(e.getMessage(), e);
        }

        if (WdaServer.isServiceRunning(statusUrl, 5000) && !device.getDeviceStatus().equals(DeviceStatusHelper.DISCONNECTED)) {
            LOG.info("There is no need to create new server - Appium already on the mentioned port.");
            return serverUrl;
        } else {
            unlockDevice(device.getUdid());
            String logLevelValue = "error:debug";
            String appiumLogFolder = String.format("./build/android/%s", device.getUdid());
            new File(appiumLogFolder).mkdirs();
            String appiumLogPath = appiumLogFolder + "/appium.log";
            deleteAppOnDevice(device.getUdid());

            DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
            desiredCapabilities.setCapability(MobileCapabilityType.UDID, device.getUdid());

            AppiumServiceBuilder appiumServiceBuilder = new AppiumServiceBuilder()
                    .withLogFile(new File(appiumLogPath))
                    .withIPAddress(IP_ADDRESS)
                    .usingPort(device.getServerPort())
                    .withArgument(SESSION_OVERRIDE)
                    .withCapabilities(desiredCapabilities)
                    .withArgument(GeneralServerFlag.LOG_TIMESTAMP)
                    .withArgument(GeneralServerFlag.LOCAL_TIMEZONE)
                    .withArgument(GeneralServerFlag.LOG_LEVEL, logLevelValue)
                    .withArgument(AndroidServerFlag.CHROME_DRIVER_PORT, Integer.toString(device.getWebPort()))
                    .withArgument(AndroidServerFlag.BOOTSTRAP_PORT_NUMBER, Integer.toString(device.getDriverPort()));
            AppiumDriverLocalService appiumDriverLocalService =
                    AppiumDriverLocalService.buildService(appiumServiceBuilder);

            appiumDriverLocalService.addOutPutStream(new AppiumLogOutputStream());

            appiumDriverLocalService.start();
            final DeviceUpdater deviceUpdater = new DeviceUpdater(repository);
            if (appiumDriverLocalService.isRunning()) {
                getServicePool().getServiceList().add(appiumDriverLocalService);
                String appiumUrlMsg = "Android Appium URL: " + appiumDriverLocalService.getUrl();
                LOG.warn(appiumUrlMsg);
                deviceUpdater.setStatus(device, IDLE);
                return appiumDriverLocalService.getUrl();
            }
            deviceUpdater.setStatus(device, NEED_SETUP);
            return null;
        }
    }

    private static void deleteAppOnDevice(String udid) {
        if (getBlackListBundleIds().length == 0) {
            LOG.error("There is no blacklisted applications found in `config.properties` file. Nothing to uninstall;");
            return;
        }
        try {
            Process process = Runtime.getRuntime().exec("adb -s " + udid + " shell pm list packages");
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            doUninstall(udid, stdInput);
        } catch (IOException e) {
            LOG.trace(e.getMessage(), e);
        }
    }

    private static void doUninstall(String udid, BufferedReader stdInput) throws IOException {
        String deviceString;
        while ((deviceString = stdInput.readLine()) != null) {
            if (deviceString.split(":").length > 0) {
                String appString = deviceString.split(":")[1];
                proceedIfBlacklisted(udid, appString);
            }
        }
    }

    private static void proceedIfBlacklisted(String udid, String appString) throws IOException {
        for (String blackListBundleId : getBlackListBundleIds()) {
            if (appString.contains(blackListBundleId)) {
                String uninstallingAppMsg = "✓ uninstalled black listed application with bundleID: " + appString;
                Process uninstallProcess = Runtime.getRuntime().exec(String.format("adb -s %s uninstall %s", udid, appString));
                BufferedReader uninstallStdInput = new BufferedReader(new InputStreamReader(uninstallProcess.getInputStream()));
                String result;
                while ((result = uninstallStdInput.readLine()) != null) {
                    if (result.contains("Success")) {
                        LOG.info(uninstallingAppMsg);
                    }
                }
            }
        }
    }

    private static void unlockDevice(String udid) {
        String wakeupCmd = String.format(adbShellPrefix, udid, "input keyevent KEYCODE_WAKEUP");
        String initUnlockCmd = String.format(adbShellPrefix, udid, "input keyevent 82");
        String enterUnlockCodeCmd = String.format(adbShellPrefix, udid, "input text 0000");
        String submitUnlockCodeCmd = String.format(adbShellPrefix, udid, "input keyevent 66");
        if (isScreenLocked(udid)) {
            try {
                Process p = Runtime.getRuntime().exec(wakeupCmd);
                p.waitFor();
                p = Runtime.getRuntime().exec(initUnlockCmd);
                p.waitFor();
                p = Runtime.getRuntime().exec(enterUnlockCodeCmd);
                p.waitFor();
                p = Runtime.getRuntime().exec(submitUnlockCodeCmd);
                p.waitFor();
            } catch (IOException | InterruptedException e) {
                LOG.error(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean isScreenLocked(String udid) {
        if (udid.startsWith("emulator")) {
            LOG.warn("Impossible to unlock screen for emulator. Please make sure it's unlocked before the test manually.");
            return false;
        }
        try {
            String command = String.format(adbShellPrefix, udid, "dumpsys nfc | grep 'mScreenState='");
            Process p = Runtime.getRuntime().exec(command);
            if (p != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    String[] value = line.split("=");
                    if (value.length > 0 & value[1].contains("_LOCKED")) {
                        return true;
                    }
                }
                p.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
        return false;
    }
}
