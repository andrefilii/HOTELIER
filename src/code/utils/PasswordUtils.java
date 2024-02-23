package code.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtils {

    /**
     * Effettua l'hashing della password passata, utilizzando l'algoritmo SHA-256
     * @param password la password in chiaro
     * @return l'hash della password
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(password.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Confronta una password in chiaro con un hash
     * @param password password in chiaro
     * @param hash hash di una password
     * @return {@code true} se l'hash è quello della password passata, {@code false} altrimenti
     */
    public static boolean checkPassword(String password, String hash) {
        try {
            return hashPassword(password).equals(hash);
        } catch (NullPointerException e) {
            return false;
        }
    }

}
