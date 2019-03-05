package io.ochubey.appium;

import io.appium.java_client.remote.MobileCapabilityType;

/**
 * Created by o.chubey on 4/19/18.
 */
public interface AllCapabilityType extends MobileCapabilityType {
    /**
     * If provided, Appium will connect to an existing WebDriverAgent instance at this URL instead of starting a new one.
     * e.g., http://localhost:8100
     * Needed for iOS tests only
     */
    String WEB_DRIVER_AGENT_URL = "webDriverAgentUrl";
}
