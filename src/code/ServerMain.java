package code;

import code.core.ConncetionListener;
import code.utils.AppConfig;

import java.io.File;

public class ServerMain {

    /**
     * Permette di controllare i prerequisiti richiesti affinchè l'applicazione possa funzionare correttamente
     * @return true se sono soddisfatti, false altrimenti
     */
    private static boolean controllaPrerequisiti() {
        File file;

        // controllo che esista il file delle proprietà nella root del progetto
        file = new File("application.properties");
        if (!file.exists() || !file.canRead()) return false;

        // controllo che esista il file Hotels.json nel percorso del database definito dalla proprietà database.url
        file = new File(AppConfig.getDatabaseUrl() + "Hotels.json");
        if (!file.exists() || !file.canRead() || !file.canWrite()) return false;

        return true;
    }

    public static void main(String[] args) {
        if(!controllaPrerequisiti()) {
            throw new RuntimeException("ERRORE! Il programma per avviarsi correttamente deve avere:\n" +
                    "- il file application.properties presente nella stessa cartella del JAR/progetto\n" +
                    "- il file Hotels.json presente nel path specificato dalla propreità database.url nel file application.properties");
        }
        new ConncetionListener().start();
    }
}
