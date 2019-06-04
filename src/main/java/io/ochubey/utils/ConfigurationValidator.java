package io.ochubey.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class ConfigurationValidator {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationValidator.class);

    private static final String CONFIG_PROPERTIES_FILE = "config.properties";

    private static final String PRODUCT_BUNDLE_IDENTIFIER = "PRODUCT_BUNDLE_IDENTIFIER";
    private static final String BLACK_LIST_BUNDLE_IDS = "BLACK_LIST_BUNDLE_IDS";
    private static final String DEVICE_POOL_TIMEOUT = "DEVICE_POOL_TIMEOUT";
    private static final String CODE_SIGN_IDENTITY = "CODE_SIGN_IDENTITY";
    private static final String DEVELOPMENT_TEAM = "DEVELOPMENT_TEAM";
    private static final String USE_EMULATORS = "USE_EMULATORS";
    private static String productBundleIdentifier;
    private static String[] blackListBundleIds;
    private static int devicePoolTimeoutMills;
    private static boolean shouldUseEmulators;
    private static String codeSignIdentity;
    private static String devTeam;
    private Properties prop;

    public ConfigurationValidator() {
        prop = new Properties();
        if (!configurationExist()) {
            createConfiguration();
        }

        String blackListedAppsProperty = prop.getProperty(BLACK_LIST_BUNDLE_IDS) != null ?
                prop.getProperty(BLACK_LIST_BUNDLE_IDS) : "";
        setDevicePoolTimeoutMills(Integer.parseInt(prop.getProperty(DEVICE_POOL_TIMEOUT)));
        setBlackListBundleIds(blackListedAppsProperty.split(","));
        setShouldUseEmulators(Boolean.parseBoolean(prop.getProperty(USE_EMULATORS)));
        setProductBundleIdentifier(prop.getProperty(PRODUCT_BUNDLE_IDENTIFIER));
        setCodeSignIdentity(prop.getProperty(CODE_SIGN_IDENTITY));
        setDevTeam(prop.getProperty(DEVELOPMENT_TEAM));
    }

    public static String[] getBlackListBundleIds() {
        return blackListBundleIds;
    }

    private static void setBlackListBundleIds(String[] blackListBundleIds) {
        ConfigurationValidator.blackListBundleIds = blackListBundleIds;
    }

    public static boolean isShouldUseEmulators() {
        return shouldUseEmulators;
    }

    private static void setShouldUseEmulators(boolean shouldUseEmulators) {
        ConfigurationValidator.shouldUseEmulators = shouldUseEmulators;
    }

    public static int getDevicePoolTimeoutMills() {
        return devicePoolTimeoutMills;
    }

    private static void setDevicePoolTimeoutMills(int devicePoolTimeoutMills) {
        ConfigurationValidator.devicePoolTimeoutMills = devicePoolTimeoutMills;
    }

    public static String getDevTeam() {
        return StringUtils.wrap(devTeam, "\"");
    }

    private static void setDevTeam(String devTeam) {
        ConfigurationValidator.devTeam = devTeam;
    }

    public static String getCodeSignIdentity() {
        return StringUtils.wrap(codeSignIdentity, "\"");
    }

    private static void setCodeSignIdentity(String codeSignIdentity) {
        ConfigurationValidator.codeSignIdentity = codeSignIdentity;
    }

    public static String getProductBundleIdentifier() {
        return StringUtils.wrap(productBundleIdentifier, "\"");
    }

    private static void setProductBundleIdentifier(String productBundleIdentifier) {
        ConfigurationValidator.productBundleIdentifier = productBundleIdentifier;
    }

    private void createConfiguration() {
        try (OutputStream output = new FileOutputStream(CONFIG_PROPERTIES_FILE)) {

            prop.setProperty(DEVICE_POOL_TIMEOUT, "1000");
            prop.setProperty(DEVELOPMENT_TEAM, "0000000000");
            prop.setProperty(CODE_SIGN_IDENTITY, "iPhone Developer");
            prop.setProperty(PRODUCT_BUNDLE_IDENTIFIER, "com.facebook.WebDriverAgentRunner");
            prop.setProperty(BLACK_LIST_BUNDLE_IDS, "WebDriverAgentRunner,appium");
            prop.setProperty(USE_EMULATORS, "true");

            prop.store(output, null);
            LOG.error("Configuration file was not found and default settings were defined. Please update {} with your data and run " +
                    "service again", CONFIG_PROPERTIES_FILE);

        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    private boolean configurationExist() {
        try (InputStream input = new FileInputStream(CONFIG_PROPERTIES_FILE)) {
            prop.load(input);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public boolean validateIos() {
        final String devTeamValue = getDevTeam();
        if (TextUtils.isEmpty(devTeamValue) || devTeamValue.contains("0000000000")) {
            LOG.error("iOS Appium Server cannot be created without DEVELOPMENT_TEAM defined, please update this value in {}",
                    CONFIG_PROPERTIES_FILE);
            return false;
        }
        if (TextUtils.isEmpty(getCodeSignIdentity())) {
            LOG.error("iOS Appium Server cannot be created without CODE_SIGN_IDENTITY defined, please update this value in {}",
                    CONFIG_PROPERTIES_FILE);
            return false;
        }
        if (TextUtils.isEmpty(getProductBundleIdentifier())) {
            LOG.error("iOS Appium Server cannot be created without PRODUCT_BUNDLE_IDENTIFIER defined, please update this value in {}",
                    CONFIG_PROPERTIES_FILE);
            return false;
        }
        return true;
    }
}
