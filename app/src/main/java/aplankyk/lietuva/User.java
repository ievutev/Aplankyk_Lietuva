package aplankyk.lietuva;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private List<Place> likedPlaces;
    private List<Place> personalizedData;

    public User(String userId) {
        this.userId = userId;
        this.likedPlaces = new ArrayList<>();
        this.personalizedData = new ArrayList<>();
    }

    public User() {

    }

    public String getUserId() {
        return userId;
    }

    public List<Place> getLikedPlaces() {
        return likedPlaces;
    }

    public List<Place> getPersonalizedData() {
        return personalizedData;
    }


}
