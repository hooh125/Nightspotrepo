package com.anedma.nightspot.dto;


import android.provider.ContactsContract;

import com.google.android.gms.maps.model.LatLng;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 26/01/2018.
 */

public class Pub {

    private String name;
    private String description;
    private LatLng latLng;
    private String phone;
    private int affinity;

    public Pub(String name, String description, LatLng latLng, String phone, int affinity) {
        this.name = name;
        this.description = description;
        this.latLng = latLng;
        this.phone = phone;
        this.affinity = affinity;
    }

    public Pub(String name, String description, LatLng latLng, String phone) {
        this.name = name;
        this.description = description;
        this.latLng = latLng;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public String getPhone() {
        return phone;
    }

    public int getAffinity() {
        return affinity;
    }
}
