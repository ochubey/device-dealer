package io.ochubey.appium;

import io.appium.java_client.service.local.AppiumDriverLocalService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ochubey on 1/16/18.
 */
public class AppiumServicePool {
    private static List<AppiumDriverLocalService> serviceList;
    private static AppiumServicePool instance;

    public static synchronized AppiumServicePool getServicePool() {
        if (instance == null) {
            instance = new AppiumServicePool();
        }
        if (instance.getServiceList() == null) {
            initServiceList();
        }
        return instance;
    }

    public List<AppiumDriverLocalService> getServiceList() {
        return serviceList;
    }

    private static void initServiceList() {
        serviceList = new ArrayList<>();
    }
}
