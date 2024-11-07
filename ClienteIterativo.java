import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ClienteIterativo {
    private int cantidad_clientes;
    private static final String HOST = "localhost";
    private static final int PUERTO = 50000;
    private PublicKey publicKey;
    private Socket socket;
    private SimetricaToolKit smToolKit;
    private BigInteger g;
    private BigInteger p;
    private BigInteger gPowX; // G^x recibido del servidor
    private BigInteger llaveMaestra; // Clave compartida con el servidor
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private long totalDifi;
    private long totalVeri;
    private long totalReto;


    public ClienteIterativo( int cantidad_clientes){
        this.cantidad_clientes=cantidad_clientes;
    }

    public void proceso(){
        
        totalDifi=0;
        totalVeri=0;
        for (int i=0;i<cantidad_clientes;i++){
            int id= i;
            int idPaquete=i;
            System.out.println("Cliente: "+i);
            try {
                loadPublicKey();
                System.out.println("----Llave publica leida-----");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
                System.out.println("Error leyendo llave publica");
            }
            String llave= String.valueOf(id)+"-"+String.valueOf(idPaquete);
            long inicioReto = System.currentTimeMillis();
            establecerConexion(llave);
            long finalReto = System.currentTimeMillis(); 
            totalReto=(int) (totalReto+(finalReto - inicioReto));

            try {
                long inicioDH = System.currentTimeMillis();
                diffieHellman();
                long finDH = System.currentTimeMillis();
                totalDifi=(int) (totalDifi+(finDH - inicioDH));
            } catch (InvalidKeyException | ClassNotFoundException | IllegalBlockSizeException | BadPaddingException
                    | NoSuchAlgorithmException | NoSuchPaddingException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.out.println("ERROR EN DIFFIE HELLMAN");
            }
            long inicioConsulta = System.currentTimeMillis();
            leerMensajeCifrado();
            long finConsulta = System.currentTimeMillis();
            totalVeri=(int) (totalVeri+(finConsulta - inicioConsulta));

        }
        
        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("");
        System.out.println("Tiempo total del reto: " + (totalReto) + " ms");
        System.out.println("Tiempo para generar G, P y G^x: " + (totalDifi) + " ms");
        System.out.println("Tiempo para verificar la consulta: " + (totalVeri) + " ms");
        }
    
    public void loadPublicKey() throws Exception {
        // Leer la llave pública
        byte[] publicKeyBytes = Files.readAllBytes(Paths.get("llaves/publicKey.key"));
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.publicKey = keyFactory.generatePublic(publicSpec);

        System.out.println("Llave pública cargada correctamente.");
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void establecerConexion(String message) {
        try {
            socket=new Socket(HOST,PUERTO);
            out=new ObjectOutputStream(socket.getOutputStream());
            in=new ObjectInputStream(socket.getInputStream());
            // Cifrar el mensaje con la llave pública
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedMessage = cipher.doFinal(message.getBytes());

            // Enviar el mensaje cifrado al servidor
            out.writeObject(encryptedMessage);
            
            // Esperar la respuesta del servidor
            byte[] decryptedMessage = (byte[]) in.readObject();
            System.out.println("Mensaje recibido del servidor: " + new String(decryptedMessage));
            if (!new String(decryptedMessage).equals(message)){
                System.out.println("----SERVIDOR NO VERIFICADO-----");
                out.writeObject("ERROR");
            }
            System.out.println("----SERVIDOR VERIFICADO-----");
            out.writeObject("OK");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void diffieHellman() throws IOException, ClassNotFoundException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {

        System.out.println(" CLIENTE ENTRA A DIFFIE HELLMAN");
        // 1. Recibir G, P y G^x cifrados del servidor
        byte[] encryptedG = (byte[]) in.readObject();
        byte[] encryptedP = (byte[]) in.readObject();
        byte[] encryptedGPowX = (byte[]) in.readObject();

        // 2. Descifrar los valores usando la llave pública del servidor
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        g = new BigInteger(cipher.doFinal(encryptedG));
        p = new BigInteger(cipher.doFinal(encryptedP));
        gPowX = new BigInteger(cipher.doFinal(encryptedGPowX));

        System.out.println("Valores G, P y G^x descifrados y recibidos del servidor.");

        // 3. Generar el secreto del cliente y calcular G^y
        SecureRandom random = new SecureRandom();
        BigInteger y = new BigInteger(1024, random); // Valor secreto del cliente
        BigInteger gPowY = g.modPow(y, p); // G^y mod P

        // 4. Confirmación al servidor y enviar G^y
        out.writeObject("OK");
        out.writeObject(gPowY);
        System.out.println("Valor G^y enviado al servidor.");

        // 5. Calcular la clave compartida (G^x)^y mod P
        llaveMaestra = gPowX.modPow(y, p); // Clave compartida (G^x)^y mod P
        System.out.println("Clave compartida calculada con el servidor.");
        //System.out.println(sharedSecret);
        smToolKit=new SimetricaToolKit(llaveMaestra);
    }

    public synchronized void leerMensajeCifrado(){
        
        try {
            byte[] encryptedP = (byte[]) in.readObject();
            byte[] bytes=smToolKit.descifrar(encryptedP);
            String mensaje= new String(bytes);
            System.out.println("Lectura de mensaje cifrado simetricamente: ");
            String nuevoMensaje="";
            if (mensaje.equals("0")) {
                nuevoMensaje = "EN_OFICINA";
            } else if (mensaje.equals("1")) {
                nuevoMensaje = "RECOGIDO";
            } else if (mensaje.equals("2")) {
                nuevoMensaje = "EN_CLASIFICACION";
            } else if (mensaje.equals("3")) {
                nuevoMensaje = "DESPACHADO";
            } else if (mensaje.equals("4")) {
                nuevoMensaje = "EN_ENTREGA";
            } else if (mensaje.equals("5")) {
                nuevoMensaje = "ENTREGADO";
            } else {
                nuevoMensaje = "DESCONOCIDO";
            }
            

            System.out.println("Estado del paquete del cliente: "+nuevoMensaje);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
