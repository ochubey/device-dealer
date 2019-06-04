package io.ochubey.devices.status;

import io.ochubey.devices.Device;
import io.ochubey.devices.repository.DeviceRepository;
import io.ochubey.devices.repository.DeviceUpdater;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.ochubey.devices.status.DeviceStatusHelper.IDLE;
import static io.ochubey.devices.status.DeviceStatusHelper.IN_TEST;
import static io.ochubey.devices.status.DeviceStatusHelper.DISCONNECTED;
import static io.ochubey.devices.status.DeviceStatusHelper.TEST_INITIALIZATION;
import static io.ochubey.utils.ConfigurationValidator.getDevicePoolTimeoutMills;
import static java.lang.Thread.sleep;

public class DeviceStatusTracker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceStatusTracker.class);

    private Device device;
    private DeviceUpdater deviceUpdater;
    private DeviceRepository repository;

    public DeviceStatusTracker(Device device, DeviceRepository repository) {
        this.repository = repository;
        this.device = getDevice(device);
        this.deviceUpdater = new DeviceUpdater(repository);
    }

    private Device getDevice(Device device) {
        return repository.findByUdid(device.getUdid());
    }

    @Override
    public void run() {
        waitForTestInitialization();
        waitForTestCompletion();
        Thread.currentThread().interrupt();
    }

    /**
     * Method will wait till initialization of the tests starts.
     * This is needed to not interrupt session in case if delayed test initialization happens.
     * Would be automatically stopped after 5 minutes if test initialization fails from client side
     * Would be successfully completed as soon as isTestInitialized() return true
     */
    private void waitForTestInitialization() {
        if (!isTestInitialized()) {
            LOG.warn("Test initialization was completed immediately on device '{}', might be an issue", device.getDeviceName());
            return;
        }
        LOG.info("Test initialization started on '{}'", device.getDeviceName());
        int retries = 0;
        final int devicePoolTimeoutMills = getDevicePoolTimeoutMills();
        int retriesLimit = (5 * 60000) / devicePoolTimeoutMills; // 5 minutes to start test after device was booked
        while (isTestInitialized()) {
            try {
                sleep(devicePoolTimeoutMills);
            } catch (InterruptedException ignored) {
                LOG.error("Exception had happened during sleep on the Thread");
                Thread.currentThread().interrupt();
            }
            if (!isSessionEmpty()) {
                deviceUpdater.setStatus(DeviceStatusTracker.this.getDevice(device), IN_TEST);
                LOG.info("Test initialization was completed and test execution started on '{}'", device.getDeviceName());
                return;
            }
            retries++;
            if (retries >= retriesLimit) {
                deviceUpdater.setStatus(DeviceStatusTracker.this.getDevice(device), IDLE);
                LOG.info("Test initialization was timeout and device '{}' was moved to idle status", device.getDeviceName());
                return;
            }
        }
    }

    /**
     * Method check status of the session each devicePoolTimeoutMills
     */
    private void waitForTestCompletion() {
        if (!isInTest()) {
            if (!getDeviceStatus().equals(DISCONNECTED)) {
                LOG.warn("Test execution was completed immediately on device '{}', might be an issue", device.getDeviceName());
            }
            return;
        }
        int retries = 0;
        final int devicePoolTimeoutMills = getDevicePoolTimeoutMills();
        int retriesLimit = (60 * 60000) / devicePoolTimeoutMills; // 60 minutes for test completion
        while (isInTest()) {
            try {
                sleep(getDevicePoolTimeoutMills());
            } catch (InterruptedException ignored) {
                LOG.error("Exception had happened during sleep on the Thread");
                Thread.currentThread().interrupt();
            }
            if (isSessionEmpty()) {
                if (!getDeviceStatus().equals(DISCONNECTED)) {
                    deviceUpdater.setStatus(getDevice(device), IDLE);
                    LOG.info("Test execution was completed and device '{}' was moved to idle status", device.getDeviceName());
                }
                return;
            }
            retries++;
            if (retries >= retriesLimit) {
                deviceUpdater.setStatus(DeviceStatusTracker.this.getDevice(device), IDLE);
                LOG.error("Test execution was timeout and device '{}' was moved to idle status", device.getDeviceName());
                return;
            }
        }
    }

    private boolean isInTest() {
        return getDeviceStatus().equals(IN_TEST);
    }

    private boolean isTestInitialized() {
        return getDeviceStatus().equals(TEST_INITIALIZATION);
    }

    private String getDeviceStatus() {
        return getDevice(device).getDeviceStatus();
    }

    /**
     * Method that returns if specific Appium service still has ongoing session
     * @return is session still active
     */
    private boolean isSessionEmpty() {
        try {
            String url = String.format("http://localhost:%s/wd/hub/sessions", device.getServerPort());
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if (response == null) {
                LOG.error("Response is null - no reason to proceed");
                return true;
            } else {
                String s = response.body().string();
                return !s.contains("\"id\"");
            }
        } catch (IOException e) {
            LOG.error("Looks like device was disconnected during the test.");
            LOG.error("Check USB ports and cable states if you sure that device was not disconnected due to human mistake.");
            return true;
        }
    }
}
