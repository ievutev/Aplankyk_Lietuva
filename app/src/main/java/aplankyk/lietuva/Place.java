package aplankyk.lietuva;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class Place implements ClusterItem {
    private String title;
    private final String snippet = "";
    private double latitude;
    private double longitude;

    // Constructor without arguments for Firebase declaration
    public Place() {
    }

    public Place(double latitude, double longitude, String title) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = title;
    }


    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    // Method to get place coordinates
    @NonNull
    @Override
    public LatLng getPosition() {
        LatLng coordinates = new LatLng(getLatitude(), getLongitude());
        return coordinates;
    }


    @Override
    public String getTitle() {
        return title;
    }


    @Override
    public String getSnippet() {
        return snippet;
    }
}
