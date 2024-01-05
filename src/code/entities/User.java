package code.entities;

import code.enums.Badges;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1;

    private String username;
    private String password;
    private Integer numRecensioni = 0;


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getNumRecensioni() {
        return numRecensioni;
    }

    public void setNumRecensioni(Integer numRecensioni) {
        this.numRecensioni = numRecensioni;
    }

    public void addRecensione() {
        this.numRecensioni++;
    }

    public Badges getBadge() {
        if (numRecensioni > 0 && numRecensioni < 10) return Badges.RECENSORE;
        if (numRecensioni < 20) return Badges.REC_ESPERTO;
        if (numRecensioni < 50) return Badges.CONTRIBUTORE;
        if (numRecensioni < 100) return Badges.CON_ESPERTO;
        return Badges.CON_SUPER;
    }
}
