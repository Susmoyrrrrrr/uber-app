package com.rideshare.locationservice.service;

import com.rideshare.locationservice.dto.DriverLocationRequest;
import com.rideshare.locationservice.dto.NearByDriverResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {
    private final RedisTemplate<String,String> redisTemplate;
	private static final String DRIVER_GEO_KEY="drivers:locations";
    public void updateDriverLocation(DriverLocationRequest driverLocationRequest) {
    	log.info("updationg locations for driver:{}",driverLocationRequest.getDriverId());
    	org.springframework.data.geo.Point driverPoint=new Point(
                driverLocationRequest.getLongitude(),
                driverLocationRequest.getLatitude()
    			);
        redisTemplate.opsForGeo().add(
                DRIVER_GEO_KEY,
                driverPoint,
                driverLocationRequest.getDriverId()
        );
        log.info(" locations updated for driver:{}",driverLocationRequest.getDriverId());
    }

    public  List<NearByDriverResponse> findNearbyDriver(double latitude, double longitude, double radiusInKm) {
        log.info(" findNearbyDriver for driver:{}",latitude,longitude);
        Circle SearchArea=new Circle(
                new Point(longitude,latitude),
                new Distance(radiusInKm, Metrics.KILOMETERS)
        );
        GeoResults<RedisGeoCommands.GeoLocation<String>> results=
                redisTemplate.opsForGeo().radius(
                        DRIVER_GEO_KEY,
                        SearchArea,
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeCoordinates()
                                .includeDistance().sortAscending().limit(10)
                );
        List<NearByDriverResponse> nearByDrivers=new ArrayList<>();
        if (results!=null) {
            results.getContent().forEach(result->{
                RedisGeoCommands.GeoLocation<String> location=result.getContent();
                nearByDrivers.add(new NearByDriverResponse(
                        location.getName(),
                        location.getPoint().getY(),
                        location.getPoint().getX(),
                        result.getDistance().getValue()
                ));
            });
        }
        log.info(" found nearby drivers:{}",nearByDrivers.size());
        return nearByDrivers;


    }

    public void removeDriver(String driverID) {
        log.info("removing driver:{}",driverID);
        redisTemplate.opsForGeo().remove(DRIVER_GEO_KEY,driverID);
    }
}
