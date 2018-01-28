package com.anedma.nightspot.dto;


import android.provider.ContactsContract;

import com.google.android.gms.maps.model.LatLng;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 26/01/2018.
 */

public class Pub {

    private String name;
    private String description;
    private String address;
    private LatLng latLng;
    private String phone;

    public Pub(String name, String description, String address, LatLng latLng, String phone) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.latLng = latLng;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public String getPhone() {
        return phone;
    }
}
