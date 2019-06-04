package io.ochubey.devices.status;

/**
 * Created by ochubey on 1/13/18.
 */
public final class DeviceStatusHelper {

    public static final String NEED_SETUP = "need_setup";
    public static final String IDLE = "idle";
    public static final String DISCONNECTED = "disconnected";
    public static final String IN_TEST = "in test";
    public static final String TEST_INITIALIZATION = "test initialization";

    private DeviceStatusHelper() {
        throw new IllegalStateException("Utility class");
    }

}
