package com.anedma.nightspot;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.anedma.nightspot.dto.Pub;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 07/02/2018.
 */

public class PubInfoDialog extends DialogFragment implements OnMapReadyCallback {

    private Pub pub;

    public static PubInfoDialog newInstance(Pub pub) {
        PubInfoDialog dialog = new PubInfoDialog();
        dialog.setPub(pub);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.pub_info_dialog, null);
        loadTextViews(view);
        builder.setView(view);
        SupportMapFragment map = (SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.map_pub_info_dialog);
        map.getMapAsync(this);
        Dialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    private void loadTextViews(View view) {
        TextView tvPubName = view.findViewById(R.id.tv_pub_name_dialog);
        TextView tvPubDescription = view.findViewById(R.id.tv_pub_description_dialog);
        TextView tvPubAddress = view.findViewById(R.id.tv_pub_address_dialog);
        TextView tvPubPhone = view.findViewById(R.id.tv_pub_phone_dialog);
        TextView tvPubTracks = view.findViewById(R.id.tv_pub_tracks_dialog);
        TextView tvPubAffinity = view.findViewById(R.id.tv_pub_affinity_dialog);
        Button buttonGoMaps = view.findViewById(R.id.button_go_maps_pub_dialog);
        Button buttonCall= view.findViewById(R.id.button_call_pub_dialog);
        if (pub != null) {
            tvPubName.setText(pub.getName());
            tvPubDescription.setText(pub.getDescription());
            tvPubAddress.setText(pub.getAddress());
            tvPubPhone.setText(pub.getPhone());
            tvPubTracks.setText(String.valueOf(pub.getTracks()));
            String affinity = pub.getAffinity();
            int resValue;
            if(affinity != null) {
                switch (affinity) {
                    case "low":
                        resValue = R.string.affinity_low;
                        break;
                    case "middle":
                        resValue = R.string.affinity_middle;
                        break;
                    case "middle-high":
                        resValue = R.string.affinity_middlehigh;
                        break;
                    case "high":
                        resValue = R.string.affinity_high;
                        break;
                    default:
                        resValue = R.string.affinity_none;
                }
            } else {
                resValue = R.string.affinity_none;
            }
            Log.d("DIALOG", "Poniendo " + getResources().getString(resValue) + " de valor");
            tvPubAffinity.setText(resValue);
            buttonGoMaps.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LatLng position = pub.getLatLng();
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + position.latitude + "," + position.longitude);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        Toast.makeText(getActivity(), R.string.warning_must_have_gmaps, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            buttonCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String phone = pub.getPhone();
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                    startActivity(intent);
                }
            });
        }
    }

    public void setPub(Pub pub) {
        this.pub = pub;
    }

    @Override
    public void onDestroyView() {
        Log.d("DIALOG", "Calling onDestroyView");
        SupportMapFragment fragment = (SupportMapFragment) (getFragmentManager().findFragmentById(R.id.map_pub_info_dialog));
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.remove(fragment);
        ft.commit();
        super.onDestroyView();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        if (pub != null) {
            map.addMarker(new MarkerOptions().position(pub.getLatLng()));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(pub.getLatLng())
                    .zoom(17).build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }
}
