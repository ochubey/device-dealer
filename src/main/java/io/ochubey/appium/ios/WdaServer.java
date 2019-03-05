package io.ochubey.appium.ios;

import io.ochubey.devices.Device;
import io.ochubey.devices.status.DeviceStatuses;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.net.UrlChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.ochubey.utils.ConfigurationValidator.*;
import static java.lang.String.format;

/**
 * Created by ochubey on 12/5/17.
 */
public class WdaServer {

    private static final Logger LOG = LoggerFactory.getLogger(WdaServer.class);
    private static final int WDA_BUILD_TIMEOUT = 120000;
    private static final int STATUS_QUICK_CHECK_TIMEOUT = 5000;
    private static final String LOCAL_IP = "127.0.0.1";

    private String deviceId;
    private String platformVersion;
    //TODO: get this value by executing `which iproxy`
    private File iproxyExecutable = new File("/usr/local/bin/iproxy");
    //TODO: get this value by executing `which xcodebuild`
    private File xcodebuildExecutable = new File("/usr/bin/xcodebuild");
    private String wdaProjectPath = "/usr/local/lib/node_modules/appium/node_modules/appium-xcuitest-driver/WebDriverAgent/";
    private File wdaProject = new File(wdaProjectPath + "WebDriverAgent.xcodeproj");

    private static File iproxyLog;
    private static File xcodebuildLog;
    private String buildScript;

    public WdaServer(Device device) {
        String deviceFolder = format("./build/ios/%s/", device.getUdid());
        new File(deviceFolder).mkdirs();
        iproxyLog = new File(format("%s/iproxy.log", deviceFolder));
        xcodebuildLog = new File(format("%s/build.log", deviceFolder));
    }

    @NotNull
    @Contract(" -> new")
    private String[] getWdaPrebiuldScript() {
        return new String[]{
                format("cd %s;", wdaProjectPath),
                "mkdir -p Resources/WebDriverAgent.bundle;",
                "./Scripts/bootstrap.sh -d;"
        };
    }

    public String getWdaUrl(Device device) {
        try {
            int driverPort = device.getDriverPort();
            String wdaStatusUrl = "http://%s:%s/%s";
            String wdaUrl = "http://%s:%s/";
            URL status = new URL(format(wdaStatusUrl, LOCAL_IP, driverPort, "status"));
            if (isServiceRunning(status, STATUS_QUICK_CHECK_TIMEOUT) && !device.getDeviceStatus().equals(DeviceStatuses.DISCONNECTED)
            ) {
                LOG.error("There is no need to rebuild - WDA already on the mentioned port.");
                return format(wdaUrl, driverPort);
            }
            deviceId = device.getUdid();
            platformVersion = device.getPlatformVersion();

            File scriptFile = File.createTempFile("script", ".sh");
            List<String> list = getWdaBuildScript(driverPort, deviceId);
            try (Writer output = new BufferedWriter(new FileWriter(scriptFile))) {
                output.write(String.join("\n", list));
            }

            ProcessBuilder pb = new ProcessBuilder("/bin/bash", scriptFile.getCanonicalPath());
            Map<String, String> env = pb.environment();
            env.put("USE_PORT", Integer.toString(driverPort));

            Process pr = pb.redirectErrorStream(true).start();
            pr.waitFor(STATUS_QUICK_CHECK_TIMEOUT, TimeUnit.MILLISECONDS);
            Path scriptFilePath = scriptFile.toPath();

            if (!isServiceRunning(status, WDA_BUILD_TIMEOUT)) {
                LOG.error(format("WDA server has failed to start after %s timeout on server '%s'. " +
                                "Please make sure that %s (%s) is properly connected and you can build WDA manually from XCode.",
                        30000, LOCAL_IP, device.getDeviceName(), device.getUdid()));
                LOG.error("Please follow instructions described on \n" +
                        "https://github.com/imurchie/appium-xcuitest-driver/blob/isaac-rs/docs/real-device-config.md");
                LOG.error("To debug the issue run following command from terminal: ");
                killIproxy(driverPort);
                return null;
            }
            if (Files.deleteIfExists(scriptFilePath)) {
                LOG.info("Temporary file was successfully deleted");
            }
            return format(wdaUrl, LOCAL_IP, driverPort);
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            return null;
        }
    }

    private void killIproxy(int driverPort) {
        Process p;
        try {
            p = Runtime.getRuntime().exec(getKillIproxyCmdline(driverPort));
            p.waitFor();
        } catch (IOException | InterruptedException ignored) {
        }
    }

    private List<String> getWdaBuildScript(int driverPort, String deviceId) {
        List<String> scriptContent = new ArrayList<>();
        scriptContent.add("#!/bin/bash");
        scriptContent.add(String.join(" ", getRemoveWdaCmdLine(deviceId)));
        scriptContent.add(String.join(" ", getKillIproxyCmdline(driverPort)));
        scriptContent.add(String.join(" ", getWdaPrebiuldScript()));
        scriptContent.add(String.join(" ", getIproxyCmdline(driverPort, deviceId)));
        scriptContent.add(String.join(" ", generateXcodebuildCmdline()));
        buildScript = String.join(" ", scriptContent);
        return scriptContent;
    }

    @NotNull
    @Contract("_ -> new")
    private String[] getRemoveWdaCmdLine(String deviceId) {
        return new String[]{
                "ios-deploy",
                format("-i %s", deviceId),
                "--uninstall_only",
                "--bundle_id com.apple.test.WebDriverAgentRunner-Runner;"
        };
    }

    public static boolean isServiceRunning(URL status, int timeout) {
        try {
            new UrlChecker().waitUntilAvailable(timeout, TimeUnit.MILLISECONDS, status);
            return true;
        } catch (UrlChecker.TimeoutException e) {
            return false;
        }
    }

    private List<String> generateXcodebuildCmdline() {
        final List<String> result = new ArrayList<>();
        result.add(xcodebuildExecutable.getAbsolutePath());
        result.add("clean build test");
        result.add(format("-project %s", wdaProject.getAbsolutePath()));
        result.add("-scheme WebDriverAgentRunner");
        result.add(format("-destination id=%s", deviceId));
        result.add("-configuration Debug");
        result.add(format("IPHONEOS_DEPLOYMENT_TARGET=%s", platformVersion));
        result.add(format("DEVELOPMENT_TEAM=%s", getDevTeam()));
        result.add(format("CODE_SIGN_IDENTITY=%s", getCodeSignIdentity()));
        result.add(format("PRODUCT_BUNDLE_IDENTIFIER=%s", getProductBundleIdentifier()));
        result.add(format("> %s", xcodebuildLog.getAbsolutePath()));
        return result;
    }

    @NotNull
    @Contract("_ -> new")
    private String[] getKillIproxyCmdline(int driverPort) {
        return new String[]{
                format("lsof -i tcp:%s", Integer.toString(driverPort)),
                "| grep iproxy",
                "| awk '{print $2}'",
                "| xargs kill;"
        };
    }

    @NotNull
    @Contract("_, _ -> new")
    private String[] getIproxyCmdline(int driverPort, String deviceId) {
        return new String[]{
                iproxyExecutable.getAbsolutePath(),
                Integer.toString(driverPort),
                Integer.toString(driverPort),
                deviceId,
                format("> %s 2>&1 &", iproxyLog.getAbsolutePath())
        };
    }
}
