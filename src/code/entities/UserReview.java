package code.entities;

public class UserReview {
    private String username;
    private Integer hotelID;
    private Double rating;
    private Ratings ratings;
    private Long timestamp;

    public UserReview(String username, Integer hotelID, Double rating, Ratings ratings, long timestamp) {
        this.username = username;
        this.hotelID = hotelID;
        this.rating = rating;
        this.ratings = ratings;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getHotelID() {
        return hotelID;
    }

    public void setHotelID(Integer hotelID) {
        this.hotelID = hotelID;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Ratings getRatings() {
        return ratings;
    }

    public void setRatings(Ratings ratings) {
        this.ratings = ratings;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
