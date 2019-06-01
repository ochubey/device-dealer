package io.ochubey.devices.repository;

import io.ochubey.devices.Device;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by o.chubey on 10/31/17.
 */
public interface DeviceExtendedRepository {

    @Query(value = "{'platform' : ?0}")
    Device bookByPlatform(@Param("platform") String platform);

    @Query(value = "{'platform' : ?0}")
    List<Device> findAllConnectedByPlatform(@Param("platform") String platform);

    List<Device> setAndroidToIdle();

    List<Device> setIosToIdle();

}
