

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
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
public class Servidor {
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private KeyGenerator generador;
    private int numeroThreads=0;
    private List<ThreadServidor> delegados = new ArrayList<>();
    private Map<String, Paquete> tabla= new HashMap<>();
    private static volatile boolean continuar = true;
    private int cantServidores;
    ServerSocket ss=null;

    private long tReto;
    private long tDH;
    private long tConsulta;

    public Servidor(KeyGenerator g){
        this.generador=g;
    }

    
    public void startServer() throws IOException{
        for (int i = 0; i <32; i++) {
            String idUsuario = String.valueOf(i);// Usuario único
            String idPaquete = String.valueOf(i); // Paquete único
            int estado = i % 6; // Ciclar entre 0 (EN_OFICINA) y 5 (ENTREGADO)
            // Crear el paquete con los datos generados
            Paquete paquete = new Paquete(idUsuario, idPaquete, estado);
            // Agregar el paquete a la tabla
            agregarPaquete(paquete);
        }
        try  {
            ss = new ServerSocket(50000);
            System.out.println("Servidor en espera de conexiones...");

        } catch (Exception e) {
            e.printStackTrace();
        }

        while(continuar){

            Socket clientSocket = ss.accept();

            // Crear un delegado para el cliente y ejecutar en un nuevo hilo
            //ThreadServidor delegado = new ThreadServidor(clientSocket, privateKey, numeroThreads);
            ThreadServidor delegado = new ThreadServidor(clientSocket, privateKey, numeroThreads, this);

            numeroThreads++;
            delegado.start();
            delegados.add(delegado);
            if(numeroThreads>=cantServidores){
                continuar=false;
            }

        }
        
        
        //ss.close();
        
        
        for (ThreadServidor delegado : delegados) {
            try {
                delegado.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        System.out.println("------Servidor terminado------");
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

    public void agregarPaquete(Paquete paquete) {
        String llave = paquete.getIdUsuario() + "-" + paquete.getIdPaquete();
        tabla.put(llave, paquete);
        
    }

    public synchronized int consultarEstado(String llave) {
        Paquete paquete = (Paquete) tabla.get(llave);

        if (paquete != null) {
            return paquete.getEstado();
        } else {
            return -1;
        }
    }

    public void selectNumero(){
        BufferedReader reader= new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Introduzca el numero de servidores concurrentes: ");
        try {
            cantServidores=Integer.parseInt(reader.readLine());
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sumarTReto(Long x){
        tReto+=x;
    }
    public synchronized void sumarTDH(Long x){
        tDH+=x;
    }
    public synchronized void sumarTConsulta(Long x){
        tConsulta+=x;
    }


    public void printMenu(){
        boolean continuar=true;
        while(continuar){
            System.out.println("Bienvenido al menu del servidor, seleccione una opcion: ");
            System.out.println("Opcion 1: Generar llaves asimetricas");
            System.out.println("Opcion 2: Seleccionar numero de servidores concurrentes");
            System.out.println("Opcion 3: Iniciar servidor");
            System.out.println("Opcion 4: Salir");

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
                        selectNumero();
                        break;
                    case"3":
                        startServer();
                        System.out.println("Tiempo total de servidores en confirmar el RETO: "+tReto+" ms");
                        System.out.println("Tiempo total de servidores en hacer Diffie Helman: "+tDH+" ms");
                        System.out.println("Tiempo total de servidores en hacer consulta de paquetes: "+tConsulta+" ms");
                        break;
                    case "4":
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
