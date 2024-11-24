package com.s333329.mappe3;

import com.google.android.gms.maps.model.LatLng;

public class Location {
    public LatLng position;
    public String description;
    public String address;
    public Double latitude;
    public Double longitude;

    public Location(LatLng position, String description, String address, Double latitude, Double longitude) {
        this.position = position;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
