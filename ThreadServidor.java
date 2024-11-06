import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.math.BigInteger;
import java.security.SecureRandom;

//import java.util.Random;

public class ThreadServidor extends Thread {
    private Socket clientSocket;
    private PrivateKey privateKey;
    private int id;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private SimetricaToolKit smToolKit;

    private static final BigInteger G = new BigInteger("2"); // Valor común, ejemplo simplificado
    private static final BigInteger P = new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"+ "29024E088A67CC74020BBEA63B139B22514A08798E3404DDE"+ "FFFFFFFFFFFFFFFF", 16); // Ejemplo de número primo grande
    
    //private BigInteger G = new BigInteger(1024, new Random());
    //private BigInteger P = BigInteger.probablePrime(1024, new Random());

    private BigInteger llaveMaestra; // Clave compartida con el cliente

    public ThreadServidor(Socket clientSocket, PrivateKey privateKey, int id) throws IOException {
        this.clientSocket = clientSocket;
        this.privateKey = privateKey;
        this.id=id;
        this.in = new ObjectInputStream(clientSocket.getInputStream());
        this.out = new ObjectOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void run() {
        
        establecerConexion();
        try {
            diffieHellman();
        } catch (InvalidKeyException | ClassNotFoundException | NoSuchAlgorithmException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mandarMensajeCifrado("Mensaje cifrado AAAAAA");
        try {
            clientSocket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
        
    }

    public void establecerConexion(){
        try {

            System.out.println("Cliente conectado: " + clientSocket.getRemoteSocketAddress()+ "en thread delegado No: "+id);

            // Leer el mensaje cifrado del cliente
            byte[] encryptedMessage = (byte[]) in.readObject();

            // Desencriptar el mensaje con la llave privada
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedMessage = cipher.doFinal(encryptedMessage);

            System.out.println("Mensaje desencriptado del cliente: " + new String(decryptedMessage));

            // Enviar el mensaje desencriptado de vuelta al cliente
            out.writeObject(decryptedMessage);

            String respuesta=(String)in.readObject();
            //System.out.println("Respuesta: "+respuesta);
            
            
            
            if (respuesta.equals("OK")){
                System.out.println("----OK RECIBIDO-----");
            }

            //in.close();
            //out.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void diffieHellman()throws ClassNotFoundException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        System.out.println("Servidor entra a diffie hellman");

        // 1. Generar el valor G^x
        SecureRandom random = new SecureRandom();
        BigInteger x = new BigInteger(1024, random); // Valor secreto del servidor
        BigInteger gPowX = G.modPow(x, P); // G^x mod P

        // 2. Cifrar G, P y G^x usando la llave privada del servidor
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] encryptedG = cipher.doFinal(G.toByteArray());
        byte[] encryptedP = cipher.doFinal(P.toByteArray());
        byte[] encryptedGPowX = cipher.doFinal(gPowX.toByteArray());

        // 3. Enviar G, P y G^x cifrados al cliente
        out.writeObject(encryptedG);
        out.writeObject(encryptedP);
        out.writeObject(encryptedGPowX);
        System.out.println("Valores G, P y G^x enviados (cifrados) al cliente.");

        // 4. Recibir confirmación y G^y del cliente
        String confirmation = (String) in.readObject(); // Recibir "OK" o "ERROR"
        BigInteger gPowY = (BigInteger) in.readObject(); // Recibir G^y del cliente

        if ("OK".equals(confirmation)) {
            // Calcular la clave compartida (G^y)^x mod P
            llaveMaestra = gPowY.modPow(x, P);
            System.out.println("Clave compartida establecida con el cliente.");
            //System.out.println(sharedSecret);
            smToolKit=new SimetricaToolKit(llaveMaestra);
        } else {
            System.out.println("Error en la autenticación del cliente.");
        }

    }

    public void mandarMensajeCifrado(String mensaje){
        byte[] bytes = mensaje.getBytes();
        try {
            out.writeObject(smToolKit.crifrar(bytes));
            System.out.println("----MENSAJE ENVIADO-----");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
