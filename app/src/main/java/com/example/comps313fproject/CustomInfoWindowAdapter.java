package com.example.comps313fproject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View infoWindowView;
    private final Context context;

    public CustomInfoWindowAdapter(Context context) {
        this.context = context;
        infoWindowView = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getInfoWindow(Marker marker) {
        render(marker, infoWindowView);
        return infoWindowView;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @SuppressLint("SetTextI18n")
    private void render(Marker marker, View view) {
        String title = marker.getTitle();
        String snippet = marker.getSnippet();

        TextView titleTextView = view.findViewById(R.id.info_window_title);
        TextView timeTextView = view.findViewById(R.id.info_window_time);
        TextView addressTextView = view.findViewById(R.id.info_window_address);

        if (title != null) {
            titleTextView.setText(title);
        } else {
            titleTextView.setText("");
        }

        if (snippet != null) {
            String[] snippetParts = snippet.split("\n");
            if (snippetParts.length >= 2) {
                timeTextView.setText(snippetParts[0]);
                addressTextView.setText(snippetParts[1]);
            } else if (snippetParts.length == 1) {
                timeTextView.setText(snippetParts[0]);
                addressTextView.setText("");
            } else {
                timeTextView.setText("");
                addressTextView.setText("");
            }
        } else {
            timeTextView.setText("");
            addressTextView.setText("");
        }
    }
}