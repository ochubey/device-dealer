package io.ochubey.appium;

import io.appium.java_client.service.local.flags.ServerArgument;
import org.jetbrains.annotations.Contract;

/**
 * Created by o.chubey on 4/19/18.
 */
public enum DealerArguments implements ServerArgument {
    WDA_PORT("--webdriveragent-port");
    private final String arg;

    DealerArguments(String arg) {
        this.arg = arg;
    }

    @Contract(pure = true)
    @Override
    public String getArgument() {
        return arg;
    }
}
