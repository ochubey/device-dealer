package io.ochubey.devices.repository;

import io.ochubey.devices.Device;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(collectionResourceRel = "devices", path = "devices")
public interface DeviceRepository extends MongoRepository<Device, String>, DeviceExtendedRepository {

    Device bookByPlatform(@Param("platform") String platform);

    List<Device> findAllConnectedByPlatform(@Param("platform") String platform);

    List<Device> setAndroidToIdle();

    List<Device> setIosToIdle();

    @Query(value = "{'udid' :?0}")
    Device findByUdid(String udid);

    @Query(value = "{'version' :?0}")
    Device findByVersion(int version);

    @Query(value = "{'webPort' :?0}")
    Device findByWebPort(int webPort);

    @Query(value = "{'driverPort' :?0}")
    Device findByDriverPort(int driverPort);

    @Query(value = "{'serverPort' :?0}")
    Device findByServerPort(int port);
}
