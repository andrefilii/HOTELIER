package code.enums;

public enum Badges {
    RECENSORE("Recensore"),
    REC_ESPERTO("Recensore esperto"),
    CONTRIBUTORE("Contributore"),
    CON_ESPERTO("Contributore esperto"),
    CON_SUPER("Contributore super");

    private final String descrizione;

    Badges(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDescrizione() {
        return descrizione;
    }

    @Override
    public String toString() {
        return descrizione;
    }
}
