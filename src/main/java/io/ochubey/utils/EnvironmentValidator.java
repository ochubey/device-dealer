package io.ochubey.utils;

import org.apache.commons.exec.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EnvironmentValidator {

    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentValidator.class);

    public boolean isCommonEnvironmentReady() {
        return isMacOs() && isMongoDbInstalled();
    }

    private boolean isMacOs() {
        if (OS.isFamilyMac()) {
            return true;
        } else {
            LOG.error("Currently Device Dealer could be executed on Mac OS only. " +
                    "Support of other platforms is not implemented yet. Please contribute or request changes.");
            return false;
        }
    }

    public boolean isiOSEnvironmentReady() {
        return isBrewInstalled() && isNpmInstalled() && isAppiumInstalled() && isXcodeInstalled()
                && isCarthageInstalled() && isIosDeployInstalled() && isIproxyInstalled() && isIosWebkitDebugProxyInstalled();
    }

    public boolean isAndroidEnvironmentReady() {
        return isAdbInstalled() && isJavaHomeConfigured() && isAndroidConfigured() && isAndroidHomeConfigured();
    }

    private boolean isBrewInstalled() {
        String command = "brew doctor";
        String msg = "Brew is not installed. \nPlease install brew using following instructions: https://brew.sh/\n" +
                "If issue persist, please run `brew doctor` in your terminal to get more information about brew related issues";
        return isPackageInstalled(command, msg);
    }

    private boolean isNpmInstalled() {
        String command = "npm -v";
        String msg = "Node is not installed. \nPlease install it using brew command: `brew install node`";
        return isPackageInstalled(command, msg);
    }

    private boolean isXcodeInstalled() {
        String command = "xcode-select -v";
        String msg = "Xcode is not installed. " +
                "\nPlease download and install Xcode from the market. " +
                "\nAlso run xcode-select --install, to install command line developer tools";
        return isPackageInstalled(command, msg);
    }

    private boolean isAppiumInstalled() {
        String command = "appium -v";
        String msg = "Appium is not installed. \nPlease install it using brew command: `npm install -g appium@1.7.2`";
        return isPackageInstalled(command, msg);
    }

    private boolean isIproxyInstalled() {
        String command = "which iproxy";
        String msg = "iproxy is not installed. \nPlease install it using brew command: `brew install --HEAD libimobiledevice`";
        return isPackageInstalled(command, msg);
    }

    private boolean isCarthageInstalled() {
        String command = "which carthage";
        String msg = "Carthage is not installed. \nPlease install it using brew command: `brew install carthage`";
        return isPackageInstalled(command, msg);
    }

    private boolean isIosWebkitDebugProxyInstalled() {
        String command = "which ios_webkit_debug_proxy";
        String msg = "ios_webkit_debug_proxy is not installed. " +
                "\nPlease install it using brew command: `brew install --HEAD ios-webkit-debug-proxy`";
        return isPackageInstalled(command, msg);
    }

    private boolean isAdbInstalled() {
        String command = "adb devices";
        String msg = "Looks like Android Studio and SDK are not installed or not configured correctly. " +
                "\nPlease install it using instructions: https://developer.android.com/studio/";
        return isPackageInstalled(command, msg);
    }

    private boolean isMongoDbInstalled() {
        String command = "nc -zvv localhost 27017";
        String msg = "MongoDB is not installed or cannot be reached at localhost:27017. " +
                "\nPlease install it using brew command: `brew install mongodb`. " +
                "\nAnd set as service using brew command: `brew services start mongodb`";
        return isPackageInstalled(command, msg);
    }

    //Since there is two possible exit code of successful check - isIosDeployInstalled() should be implemented differently
    //and cannot reuse isPackageInstalled(command, msg)
    private boolean isJavaHomeConfigured() {
        //TODO: create validation of JAVA_HOME and JAVA_HOME/bin presence in PATH variables
        return true;
    }

    private boolean isAndroidHomeConfigured() {
        //TODO: create validation of ANDROID_HOME and JAVA_HOME/bin presence in PATH variables
        return true;
    }

    private boolean isAndroidConfigured() {
        //TODO: create validation of ANDROID_HOME/android presence in PATH variables
        return true;
    }

    private boolean isIosDeployInstalled() {
        String command = "ios-deploy -c";
        try {
            Process p = Runtime.getRuntime().exec(command);
            if (p != null) {
                final int exitValue = p.waitFor();
                if (exitValue == 0) {
                    return true;
                } else if (exitValue == 253) {
                    LOG.warn("ios-deploy is installed, but was not able to connect to any ios device via usb.");
                    return true;
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage());
            Thread.currentThread().interrupt();
        }

        LOG.error("ios-deploy is not installed. \nPlease install it using npm command: `npm install -g ios-deploy`");
        return false;
    }

    private boolean isPackageInstalled(String command, String msg) {
        try {
            Process p = Runtime.getRuntime().exec(command);
            if (p != null) {
                final int exitValue = p.waitFor();
                if (exitValue == 0) {
                    return true;
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
        LOG.error(msg);
        return false;
    }
}
