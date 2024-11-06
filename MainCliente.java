import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
public class MainCliente {

    public int numeroClientes;
    private int cantClientes;
    BufferedReader reader= new BufferedReader(new InputStreamReader(System.in));


    public static void main(String[]args){
        MainCliente mc=new MainCliente();
        mc.printMenu();
    }

    public void selectNumero(){
        System.out.println("Introduzca el numero de clientes: ");
        try {
            cantClientes=Integer.parseInt(reader.readLine());
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }

    public void iniciarClientes(){
        for (int i=0;i<cantClientes;i++){
            Cliente c=new Cliente(i,i);
            c.start();
        }
    }

    public void printMenu(){
        boolean continuar=true;
        while(continuar){
            System.out.println("Bienvenido al menu del cliente, seleccione una opcion: ");
            System.out.println("Opcion 1: Determinar numero de clientes");
            System.out.println("Opcion 2: Iniciar clientes");
            System.out.println("Opcion 3: Salir");

            try {
                String opcion=reader.readLine();
                switch(opcion){
                    case"1":
                        selectNumero();
                        break;
                    case"2":
                        iniciarClientes();
                        break;
                    case "3":
                        continuar=false;
                        System.out.println("------EJECUCION FINALIZADA-----");
                        break;
                }

            
            }catch(Exception e){}
        }
    }
}
