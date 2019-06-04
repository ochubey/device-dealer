package io.ochubey.appium.ios;

import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.appium.java_client.service.local.flags.IOSServerFlag;
import io.ochubey.appium.AllCapabilityType;
import io.ochubey.appium.AppiumLogOutputStream;
import io.ochubey.appium.AppiumServicePool;
import io.ochubey.appium.DealerArguments;
import io.ochubey.devices.Device;
import io.ochubey.devices.repository.DeviceRepository;
import io.ochubey.devices.repository.DeviceUpdater;
import io.ochubey.devices.status.DeviceStatusHelper;
import org.apache.http.util.TextUtils;
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
import static io.ochubey.devices.status.DeviceStatusHelper.IDLE;
import static io.ochubey.devices.status.DeviceStatusHelper.NEED_SETUP;
import static io.ochubey.utils.ConfigurationValidator.getBlackListBundleIds;

/**
 * Created by ochubey on 1/4/18.
 */
public class IosAppiumServer {

    public static final String IP_ADDRESS = (System.getProperty("ip") != null) ? System.getProperty("ip") : "127.0.0.1";
    private static final Logger LOG = LoggerFactory.getLogger(IosAppiumServer.class);

    private IosAppiumServer() {
    }

    //TODO: would be great to refactor getServerUrl to decrease number of lines in one method
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

        final DeviceUpdater deviceUpdater = new DeviceUpdater(repository);
        if (WdaServer.isServiceRunning(statusUrl, 5000) && !device.getDeviceStatus().equals(DeviceStatusHelper.DISCONNECTED)) {
            LOG.error("There is no need to create new server - Appium already on the mentioned port.");
            return serverUrl;
        } else {
            deleteAppsFromDevice(device.getUdid());
            String logLevelValue = "error:debug";
            String appiumLogFolder = String.format("./build/ios/%s", device.getUdid());
            if (new File(appiumLogFolder).mkdirs()) {
                LOG.info("Folder {} was created", appiumLogFolder);
            }
            String appiumLogPath = appiumLogFolder + "/appium.log";

            String wdaUrl = getWdaUrl(device);
            if (wdaUrl.isEmpty()) {
                LOG.error("Impossible to build wda for the device. There is no reason to start Appium server.");
                deviceUpdater.setStatus(device, NEED_SETUP);
                return null;
            }

            if (!IosWebKitServer.isIwdRunning(device)) {
                LOG.error("Impossible to setup web proxy for the device. There is no reason to build WDA and start Appium server.");
                deviceUpdater.setStatus(device, NEED_SETUP);
                return null;
            }

            DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
            desiredCapabilities.setCapability(MobileCapabilityType.UDID, device.getUdid());
            desiredCapabilities.setCapability(AllCapabilityType.WEB_DRIVER_AGENT_URL, wdaUrl);

            AppiumServiceBuilder appiumServiceBuilder = new AppiumServiceBuilder()
                    .withLogFile(new File(appiumLogPath))
                    .withIPAddress(IP_ADDRESS)
                    .usingPort(device.getServerPort())
                    .withArgument(SESSION_OVERRIDE)
                    .withCapabilities(desiredCapabilities)
                    .withArgument(GeneralServerFlag.LOG_TIMESTAMP)
                    .withArgument(GeneralServerFlag.LOCAL_TIMEZONE)
                    .withArgument(GeneralServerFlag.LOG_LEVEL, logLevelValue)
                    .withArgument(DealerArguments.WDA_PORT, Integer.toString(device.getDriverPort()))
                    .withArgument(IOSServerFlag.WEBKIT_DEBUG_PROXY_PORT, Integer.toString(device.getWebPort()));

            AppiumDriverLocalService appiumDriverLocalService =
                    AppiumDriverLocalService.buildService(appiumServiceBuilder);

            appiumDriverLocalService.addOutPutStream(new AppiumLogOutputStream());

            appiumDriverLocalService.start();
            if (appiumDriverLocalService.isRunning()) {
                AppiumServicePool.getServicePool().getServiceList().add(appiumDriverLocalService);
                String appiumUrlMsg = "iPhone Appium URL: " + appiumDriverLocalService.getUrl();
                LOG.warn(appiumUrlMsg);
                deviceUpdater.setStatus(device, IDLE);
                return appiumDriverLocalService.getUrl();
            }
        }
        deviceUpdater.setStatus(device, NEED_SETUP);
        return null;
    }

    private static void deleteAppsFromDevice(String udid) {
        if (getBlackListBundleIds().length == 0) {
            LOG.error("There is no blacklisted applications found in `config.properties` file. Nothing to uninstall;");
            return;
        }
        try {
            Process process = Runtime.getRuntime().exec("ideviceinstaller --udid " + udid + "  --list-apps -o list_user");
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String firstLine = stdInput.readLine();
            if (TextUtils.isEmpty(firstLine)) {
                LOG.error("There is no user applications found on device with udid {}. Nothing to uninstall;", udid);
            } else {
                //There is several versions of `ideviceinstaller` result representation
                // (till version 1.1.0 separator was " - ", unreleased HEAD separator is ","),
                // that is why we need to have 2 different parser logic implementation
                String infoSeparator;
                int expectedLength;

                if (firstLine.contains("CFBundleIdentifier")) {
                    infoSeparator = ", ";
                    expectedLength = 3;
                } else {
                    infoSeparator = " - ";
                    expectedLength = 2;
                }
                doUninstall(stdInput, udid, infoSeparator, expectedLength);
            }
        } catch (IOException e) {
            LOG.trace(e.getMessage(), e);
        }
    }

    private static void doUninstall(BufferedReader stdInput, String udid, String infoSeparator, int expectedLength) throws IOException {
        String deviceString;

        while ((deviceString = stdInput.readLine()) != null) {
            String[] appStrings = deviceString.split(infoSeparator);

            if (appStrings.length == expectedLength) {
                String appString = appStrings[0];
                proceedIfBlacklisted(udid, appString);
            }
        }
    }

    private static void proceedIfBlacklisted(String udid, String appString) throws IOException {
        for (String blackListBundleId : getBlackListBundleIds()) {
            if (appString.contains(blackListBundleId)) {
                String uninstallingAppMsg = "✓ uninstalled black listed application with bundleID: " + appString;
                Process uninstallProcess = Runtime.getRuntime().exec("ideviceinstaller --udid " + udid + " --uninstall " + appString);
                BufferedReader uninstallStdInput = new BufferedReader(new InputStreamReader(uninstallProcess.getInputStream()));
                String result;
                while ((result = uninstallStdInput.readLine()) != null) {
                    if (result.contains("Complete")) {
                        LOG.info(uninstallingAppMsg);
                    }
                }
            }
        }
    }

    private static String getWdaUrl(Device device) {
        WdaServer wdaServer = new WdaServer(device);
        String wdaUrl = wdaServer.getWdaUrl(device);
        if (wdaUrl != null) {
            return wdaUrl;
        } else {
            return "";
        }
    }
}
