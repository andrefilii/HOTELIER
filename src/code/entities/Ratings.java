package code.entities;

import java.io.Serializable;

public class Ratings implements Serializable {
    private static final long serialVersionUID = 1;

    private Double cleaning;
    private Double position;
    private Double services;
    private Double quality;

    public Ratings(Double cleaning, Double position, Double services, Double quality) {
        this.cleaning = cleaning;
        this.position = position;
        this.services = services;
        this.quality = quality;
    }

    public Double getCleaning() {
        return cleaning;
    }

    public void setCleaning(Double cleaning) {
        this.cleaning = cleaning;
    }

    public Double getPosition() {
        return position;
    }

    public void setPosition(Double position) {
        this.position = position;
    }

    public Double getServices() {
        return services;
    }

    public void setServices(Double services) {
        this.services = services;
    }

    public Double getQuality() {
        return quality;
    }

    public void setQuality(Double quality) {
        this.quality = quality;
    }

    @Override
    public String toString() {
        return "Ratings{" +
                "cleaning=" + cleaning +
                ", position=" + position +
                ", services=" + services +
                ", quality=" + quality +
                '}';
    }
}
