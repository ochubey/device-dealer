package io.ochubey.appium.ios;

import io.appium.java_client.service.local.flags.ServerArgument;
import org.jetbrains.annotations.Contract;

/**
 * Created by o.chubey on 4/15/18.
 */
public enum MyArguments implements ServerArgument {
    WDA_PORT("--webdriveragent-port");
    private final String arg;

    MyArguments(String arg) {
        this.arg = arg;
    }

    @Contract(pure = true)
    @Override
    public String getArgument() {
        return arg;
    }
}
