import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class SimetricaToolKit {
    private BigInteger sharedSecret;
    private byte[] encryptionKey;
    private byte[] hmacKey;
    private SecureRandom random = new SecureRandom();

    public SimetricaToolKit(BigInteger sharedSecret) {
        this.sharedSecret = sharedSecret;
        generateKeys();
    }

    private void generateKeys() {
        try {
            // 1. Calcular el digest SHA-512 de la clave compartida
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            byte[] digest = sha512.digest(sharedSecret.toByteArray());

            // 2. Partir el digest en dos mitades (256 bits cada una)
            encryptionKey = new byte[32]; // 256 bits para AES
            hmacKey = new byte[32]; // 256 bits para HMAC
            System.arraycopy(digest, 0, encryptionKey, 0, 32);
            System.arraycopy(digest, 32, hmacKey, 0, 32);

            System.out.println("Clave de cifrado AES generada.");
            System.out.println("Clave de HMAC generada.");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public byte[] crifrar(byte[] plaintext) throws Exception {
        // Generar IV para AES CBC
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Configurar AES CBC con clave de cifrado
        SecretKey aesKey = new SecretKeySpec(encryptionKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Calcular HMAC con la clave de autenticación
        SecretKey hmacSecretKey = new SecretKeySpec(hmacKey, "HmacSHA256");
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(hmacSecretKey);
        byte[] hmacValue = hmac.doFinal(ciphertext);

        // Concatenar IV, texto cifrado y HMAC
        byte[] encryptedMessage = new byte[iv.length + ciphertext.length + hmacValue.length];
        System.arraycopy(iv, 0, encryptedMessage, 0, iv.length);
        System.arraycopy(ciphertext, 0, encryptedMessage, iv.length, ciphertext.length);
        System.arraycopy(hmacValue, 0, encryptedMessage, iv.length + ciphertext.length, hmacValue.length);

        return encryptedMessage;
    }

    public byte[] descifrar(byte[] encryptedMessage) throws Exception {
        // Separar IV, texto cifrado y HMAC
        byte[] iv = new byte[16];
        byte[] hmacValue = new byte[32];
        byte[] ciphertext = new byte[encryptedMessage.length - iv.length - hmacValue.length];

        System.arraycopy(encryptedMessage, 0, iv, 0, iv.length);
        System.arraycopy(encryptedMessage, iv.length, ciphertext, 0, ciphertext.length);
        System.arraycopy(encryptedMessage, iv.length + ciphertext.length, hmacValue, 0, hmacValue.length);

        // Verificar HMAC
        SecretKey hmacSecretKey = new SecretKeySpec(hmacKey, "HmacSHA256");
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(hmacSecretKey);
        byte[] computedHmac = hmac.doFinal(ciphertext);

        if (!MessageDigest.isEqual(hmacValue, computedHmac)) {
            throw new SecurityException("HMAC ERROR");
        }

        // Configurar AES CBC con la clave de cifrado y el IV para descifrado
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKey aesKey = new SecretKeySpec(encryptionKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
        return cipher.doFinal(ciphertext);
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

    public byte[] getHmacKey() {
        return hmacKey;
    }
}
