public class Paquete {
    private String idUsuario;
    private String idPaquete;
    private int estado;

    public Paquete(String idUsuario, String idPaquete, int estado) {
        this.idUsuario = idUsuario;
        this.idPaquete = idPaquete;
        this.estado = estado;
    }
    public String getIdUsuario() {
        return idUsuario;
    }

    public String getIdPaquete() {
        return idPaquete;
    }

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    //EN_OFICINA = 0;
    //RECOGIDO = 1;
    //EN_CLASIFICACION = 2;
    //DESPACHADO = 3;
    //EN_ENTREGA = 4;
    //ENTREGADO = 5;
    //DESCONOCIDO = -1;


}
