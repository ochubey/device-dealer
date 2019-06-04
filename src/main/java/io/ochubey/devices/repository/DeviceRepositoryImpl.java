package io.ochubey.devices.repository;

import io.ochubey.devices.Device;
import io.ochubey.devices.status.DeviceStatusHelper;
import io.ochubey.devices.status.DeviceStatusTracker;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by o.chubey on 10/31/17.
 */
public class DeviceRepositoryImpl implements DeviceExtendedRepository {

    private static final String DEVICE_STATUS = "deviceStatus";
    private static final String UDID = "udid";
    private static final String PLATFORM = "platform";
    private static final String IOS = "ios";
    private static final String ANDROID = "android";
    private final ReentrantLock lock = new ReentrantLock();
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private DeviceRepository repository;

    @Override
    public Device bookByPlatform(String platform) {
        lock.lock();
        Device device = null;
        Query availableDeviceQuery = new Query(Criteria.where(PLATFORM).is(platform.toLowerCase()).and(DEVICE_STATUS).is(DeviceStatusHelper.IDLE));
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
                and(DEVICE_STATUS).ne(DeviceStatusHelper.DISCONNECTED));
        List<Device> devices = mongoTemplate.find(availableDeviceQuery, Device.class);
        lock.unlock();
        return devices;
    }

    @Override
    public List<Device> setAndroidToIdle() {
        return setPlatformToIdle(ANDROID);
    }

    @Override
    public List<Device> setIosToIdle() {
        return setPlatformToIdle(IOS);
    }

    private List<Device> setPlatformToIdle(String platform) {
        lock.lock();

        Update update = new Update();
        update.set(DEVICE_STATUS, DeviceStatusHelper.IDLE);

        Query devicesInTestQuery = new Query(Criteria.where(PLATFORM).is(platform).and(DEVICE_STATUS).is(DeviceStatusHelper.IN_TEST));
        mongoTemplate.updateMulti(devicesInTestQuery, update, Device.class);

        Query devicesInTestInitQuery = new Query(Criteria.where(PLATFORM).is(platform).and(DEVICE_STATUS).is(DeviceStatusHelper.TEST_INITIALIZATION));
        mongoTemplate.updateMulti(devicesInTestInitQuery, update, Device.class);

        lock.unlock();
        return repository.findAll();
    }

    private void bookDevice(@NotNull Device device) {
        lock.lock();
        Query deviceByUdidQuery = new Query(Criteria.where(UDID).is(device.getUdid()));
        Update update = new Update();
        update.set(DEVICE_STATUS, DeviceStatusHelper.TEST_INITIALIZATION);
        mongoTemplate.updateFirst(deviceByUdidQuery, update, Device.class);
        lock.unlock();
        DeviceStatusTracker deviceStatusTracker = new DeviceStatusTracker(device, repository);
        Thread deviceStatusTrackerThread = new Thread(deviceStatusTracker);
        deviceStatusTrackerThread.setName(device.getUdid());
        deviceStatusTrackerThread.start();
    }
}
