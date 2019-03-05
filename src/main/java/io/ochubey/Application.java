package io.ochubey;

import io.ochubey.devices.android.AndroidLocator;
import io.ochubey.devices.ios.IphoneLocator;
import io.ochubey.devices.repository.DeviceRepository;
import io.ochubey.utils.ConfigurationValidator;
import io.ochubey.utils.EnvironmentValidator;
import org.apache.commons.exec.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static io.ochubey.devices.repository.DeviceUpdater.cleanDeviceDataBaseOnStart;

@SpringBootApplication
public class Application implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static ConfigurationValidator configurationValidator;

    @Autowired
    private DeviceRepository repository;

    @Value("${server.port}")
    protected int port;

    private static final EnvironmentValidator envValidator = new EnvironmentValidator();

    public static void main(String[] args) {
        configurationValidator = new ConfigurationValidator();
        if (envValidator.isCommonEnvironmentReady()) {
            SpringApplication.run(Application.class, args);
        } else {
            LOG.error("Cannot start application");
        }
    }

    @Override
    public void run(String... args) {
        String initialMsg = String.format("Device Dealer successfully started listening to the port number %s", port);
        LOG.warn(initialMsg);
        cleanDeviceDataBaseOnStart(repository);
        initAndroidThread();
        initIphoneThread();
    }

    private void initAndroidThread() {
        if (envValidator.isAndroidEnvironmentReady()) {
            AndroidLocator androidLocator = new AndroidLocator(repository);
            Thread androidThread = new Thread(androidLocator);
            androidThread.setName("androidThread");
            androidThread.start();
        } else {
            LOG.error("Cannot start iPhone locator since environment is not ready. " +
                    "\nPlease refer to ERROR messages above.");
        }
    }

    private void initIphoneThread() {
        if (OS.isFamilyMac()) {
            if (configurationValidator.validateIos()) {
                if (envValidator.isiOSEnvironmentReady()) {
                    IphoneLocator iphoneLocator = new IphoneLocator(repository);
                    Thread iphoneThread = new Thread(iphoneLocator);
                    iphoneThread.setName("iPhoneThread");
                    iphoneThread.start();
                } else {
                    LOG.error("Cannot start iPhone locator since environment is not ready. " +
                            "\nPlease refer to ERROR messages above.");
                }
            } else {
                LOG.error("Information about developers identity needed for WDA build not found. " +
                        "iPhone device locator started would not be started.");
            }
        } else {
            LOG.warn("Only Android devices could be served on non OSX systems.");
        }
    }
}
