

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Servidor {
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private KeyGenerator generador;
    private int numeroThreads=0;
    ServerSocket ss=null;

    public Servidor(KeyGenerator g){
        this.generador=g;
    }

    
    public void startServer() throws IOException{
        try  {
            ss = new ServerSocket(50000);
            System.out.println("Servidor en espera de conexiones...");

        } catch (Exception e) {
            e.printStackTrace();
        }

        while (true) {
            // Aceptar conexión entrante
            Socket clientSocket = ss.accept();

            // Crear un delegado para el cliente y ejecutar en un nuevo hilo
            ThreadServidor delegado = new ThreadServidor(clientSocket, privateKey, numeroThreads);
            numeroThreads++;
            delegado.start();
        }
        //ss.close();
        
        
    }


    public void generarLlaves(){
        generador.generateKeyPair(); //se generan las llaves asimetricas
        System.out.println("------LLAVES GENERADAS Y GUARDADAS EXITOSAMENTE------");
    }


    public void loadKeys() throws Exception {
        // Leer la llave pública
        byte[] publicKeyBytes = Files.readAllBytes(Paths.get("llaves/publicKey.key"));
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.publicKey = keyFactory.generatePublic(publicSpec);

        // Leer la llave privada
        byte[] privateKeyBytes = Files.readAllBytes(Paths.get("llaves/privateKey.key"));
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        this.privateKey = keyFactory.generatePrivate(privateSpec);

        System.out.println("----Llaves cargadas correctamente-----");
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void printMenu(){
        boolean continuar=true;
        while(continuar){
            System.out.println("Bienvenido al menu del servidor, seleccione una opcion: ");
            System.out.println("Opcion 1: Generar llaves asimetricas");
            System.out.println("Opcion 2: Iniciar servidor");
            System.out.println("Opcion 3: Salir");

            BufferedReader reader= new BufferedReader(new InputStreamReader(System.in));
            try {
                String opcion=reader.readLine();
                switch(opcion){
                    case"1":
                        generarLlaves();
                        try {
                            loadKeys();
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("----Error cargando llaves-----");
                        }
                        break;
                    case"2":
                        startServer();
                        break;
                    case "3":
                        continuar=false;
                        System.out.println("------EJECUCION FINALIZADA-----");
                        break;
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error con Buffer Reader");
            }
        }
    }

    public static void main (String []args){

        Servidor s= new Servidor(new KeyGenerator());
        s.printMenu();
        
    }
}
