package com.anedma.nightspot.dto;


import com.anedma.nightspot.R;
import com.google.android.gms.maps.model.LatLng;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 26/01/2018.
 */

public class Pub {

    private int id;
    private String name;
    private String description;
    private LatLng latLng;
    private String phone;
    private String affinity;

    public Pub(int id, String name, String description, LatLng latLng, String phone, String affinity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.latLng = latLng;
        this.phone = phone;
        this.affinity = affinity;
    }

    public Pub(int id, String name, String description, LatLng latLng, String phone) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.latLng = latLng;
        this.phone = phone;
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

    public String getAffinity() {
        return affinity;
    }

    public int getResourceFromAffinity() {
        int resource = -1;
        if(!affinity.isEmpty()) {
            switch (affinity) {
                case "high":
                    resource = R.drawable.ic_map_marker_green;
                    break;
                case "middle-high":
                    resource = R.drawable.ic_map_marker_lightgreen;
                    break;
                case "middle":
                    resource = R.drawable.ic_map_maker_yellow;
                    break;
                case "low":
                    resource = R.drawable.ic_map_marker_orange;
                    break;
                default:
                    resource = R.drawable.ic_map_marker_grey;
                    break;
            }
        }
        return resource;
    }

    public int getId() {
        return id;
    }
}
