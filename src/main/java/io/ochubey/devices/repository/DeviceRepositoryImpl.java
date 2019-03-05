package io.ochubey.devices.repository;

import io.ochubey.devices.Device;
import io.ochubey.devices.android.AndroidHelper;
import io.ochubey.devices.status.DeviceStatusTracker;
import io.ochubey.devices.status.DeviceStatuses;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static io.appium.java_client.remote.MobilePlatform.ANDROID;

/**
 * Created by o.chubey on 10/31/17.
 */
public class DeviceRepositoryImpl implements DeviceExtendedRepository {

    private static final String DEVICE_STATUS = "deviceStatus";
    private static final String UDID = "udid";
    private static final String PLATFORM = "platform";
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    DeviceRepository repository;

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public Device bookByPlatform(String platform) {
        lock.lock();
        Device device = null;
        Query availableDeviceQuery = new Query(Criteria.where(PLATFORM).is(platform.toLowerCase()).and(DEVICE_STATUS).is(DeviceStatuses.IDLE));
        List<Device> devices = mongoTemplate.find(availableDeviceQuery, Device.class);
        if ((devices != null) && (!devices.isEmpty())) {
            device = devices.get(0);
            bookDevice(device);
        }
        lock.unlock();
        return device;
    }

    @Override
    public List<Device> findAllConnectedByPlatform(String platform) {
        lock.lock();
        Query availableDeviceQuery = new Query(Criteria.where(PLATFORM).is(platform.toLowerCase()).
                and(DEVICE_STATUS).ne(DeviceStatuses.DISCONNECTED));
        List<Device> devices = mongoTemplate.find(availableDeviceQuery, Device.class);
        lock.unlock();
        return devices;
    }

    private void bookDevice(@NotNull Device device) {
        lock.lock();
        Query deviceByUdidQuery = new Query(Criteria.where(UDID).is(device.getUdid()));
        Update update = new Update();
        update.set(DEVICE_STATUS, DeviceStatuses.TEST_INITIALIZATION);
        mongoTemplate.updateFirst(deviceByUdidQuery, update, Device.class);
        lock.unlock();
        DeviceStatusTracker deviceStatusTracker = new DeviceStatusTracker(device, repository);
        Thread deviceStatusTrackerThread = new Thread(deviceStatusTracker);
        deviceStatusTrackerThread.setName(device.getUdid());
        deviceStatusTrackerThread.start();
    }
}
