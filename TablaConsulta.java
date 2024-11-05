
import java.util.HashMap;
import java.util.Map;

public class TablaConsulta {
    private Map<String, Map<String, Paquete>> tabla= new HashMap<>();

    public void agregarPaquete(Paquete paquete) {
        String idUsuario = paquete.getIdUsuario() + "-" + paquete.getIdPaquete();
        Map<String, Paquete> paquetes_1cliente= new HashMap<>();
        paquetes_1cliente.put(paquete.getIdPaquete(),paquete);
        tabla.put(idUsuario, paquetes_1cliente);
        
    }

    public int consultarEstado(String idUsuario, String idPaquete) {
        Paquete paquete = (Paquete) tabla.get(idUsuario).get(idPaquete);
        if (paquete != null) {
            return paquete.getEstado();
        } else {
            return -1;
        }
    }
}
