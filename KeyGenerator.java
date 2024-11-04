import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyGenerator {

    
    public void generateKeyPair() {
        try {
            // Generación del par de llaves (RSA de 1024 bits)
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair pair = keyGen.generateKeyPair();

            // Obtención de las llaves pública y privada
            PublicKey publicKey = pair.getPublic();
            PrivateKey privateKey = pair.getPrivate();

            // Crear la carpeta "llaves" si no existe
            File directory = new File("llaves");
            if (!directory.exists()) {
                directory.mkdir();
            }

            // Guardar la llave pública en la carpeta "llaves"
            try (FileOutputStream fos = new FileOutputStream("llaves/publicKey.key")) {
                fos.write(publicKey.getEncoded());
            }

            // Guardar la llave privada en la carpeta "llaves"
            try (FileOutputStream fos = new FileOutputStream("llaves/privateKey.key")) {
                fos.write(privateKey.getEncoded());
            }

            System.out.println("Par de llaves generado y almacenado en la carpeta 'llaves'.");

            // En una instalación real, configurar permisos de archivo:
            // La llave privada debería estar protegida solo para el propietario.
            // La llave pública podría estar en un directorio accesible para los clientes.
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }
}

