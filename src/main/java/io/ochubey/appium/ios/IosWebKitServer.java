package io.ochubey.appium.ios;

import io.ochubey.devices.Device;
import io.ochubey.devices.status.DeviceStatuses;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import static io.ochubey.appium.ios.IosAppiumServer.IP_ADDRESS;
import static io.ochubey.devices.repository.DeviceUpdater.killProcessByPort;

/**
 * Created by ochubey on 1/10/18.
 */
public class IosWebKitServer {

    private static final Logger LOG = LoggerFactory.getLogger(IosWebKitServer.class);

    private IosWebKitServer() {
        throw new IllegalStateException("Utility class");
    }

    protected static boolean isIwdRunning(@NotNull Device device) {
        int webPort = device.getWebPort();
        String udid = device.getUdid();

        URL serverUrl = getUrl(webPort);

        if (WdaServer.isServiceRunning(serverUrl, 5000) && !device.getDeviceStatus().equals(DeviceStatuses.DISCONNECTED)) {
            LOG.error("There is no need to create new session - IWD is already on the mentioned port.");
            return true;
        } else {

            killProcessByPort(webPort);

            String command = getIwdCmdLine(webPort, udid);

            File scriptFile;

            try {
                scriptFile = File.createTempFile("script", ".sh");
                try (Writer output = new BufferedWriter(new FileWriter(scriptFile))) {
                    output.write(command);
                }
                ProcessBuilder pb = new ProcessBuilder("/bin/bash", scriptFile.getCanonicalPath());
                Process pr = pb.redirectErrorStream(true).start();
                pr.waitFor();
            } catch (IOException | InterruptedException e) {
                LOG.trace(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
        return WdaServer.isServiceRunning(serverUrl, 5000);
    }

    @Nullable
    private static URL getUrl(int webPort) {
        String serverStringUrl = String.format("http://%s:%s", IP_ADDRESS, webPort);

        try {
            return new URL(serverStringUrl);
        } catch (MalformedURLException ignored) {
            return null;
        }
    }

    private static String getIwdCmdLine(int webPort, String udid) {
        String appiumLogFolder = String.format("./build/ios/%s/", udid);
        new File(appiumLogFolder).mkdirs();
        return String.format("ios_webkit_debug_proxy -c %s:%s > %s/iwd.log 2>&1 &", udid, webPort, appiumLogFolder);
    }
}
