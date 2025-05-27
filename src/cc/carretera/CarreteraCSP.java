package cc.carretera;

import org.jcsp.lang.*;

public class CarreteraCSP implements Carretera, CSProcess {
  // TODO: Declaración de canales
  // Ej. private Any2One chOp;

  // Configuración de la carretera
  private final int segmentos;
  private final int carriles;

  public CarreteraCSP(int segmentos, int carriles) {
    this.segmentos = segmentos;
    this.carriles = carriles;

    // TODO: Creación de canales para comunicación con el servidor
    // Ej. chOp = Channel.any2one();

    // Puesta en marcha del servidor: alternativa sucia (desde el
    // punto de vista de CSP) a Parallel que nos ofrece JCSP para
    // poner en marcha un CSProcess
    new ProcessManager(this).start();
  }

  public Pos entrar(String car, int tks) {
    // TODO: código que ejecuta el cliente para enviar/recibir un
    // mensaje al server para que ejecute entrar
    return null;
  }

  public Pos avanzar(String car, int tks) {
    // TODO: código que ejecuta el cliente para enviar/recibir un
    // mensaje al server para que ejecute avanzar
    return null;
  }

  public void salir(String car) {
    // TODO: código que ejecuta el cliente para enviar un mensaje al
    // server para que ejecute salir
  }

  public void circulando(String car) {
    // TODO: código que ejecuta el cliente para enviar un mensaje al
    // server para que ejecute circulando
  }

  public void tick() {
    // TODO: código que ejecuta el cliente para enviar un mensaje al
    // server para que ejecute tick
  }

  // Código del servidor
  public void run() {
    // TODO: declaración e inicialización del estado del recurso

    // TODO: declaración e inicialización de estructuras de datos para
    // almacenar peticiones de los clientes

    // TODO: declaración e inicialización de arrays necesarios para
    // poder hacer la recepción no determinista (Alternative)

    // TODO: cambiar null por el array de canales
    Alternative servicios = new Alternative(null);

    // Bucle principal del servicio
    while(true){
      // TODO: declaración de variables auxiliares
      int servicio;

      // TODO: cálculo de las guardas

      // TODO: cambiar null por el array de guardas
      servicio = servicios.fairSelect(null);

      // TODO: ejecutar la operación solicitada por el cliente
      switch (servicio){
      case 0:
        // TODO: ejecutar operación 0 o almacenar la petición y
        // responder al cliente si es posible

        break;
      }

      // TODO: atender peticiones pendientes que puedan ser atendidas
    }
  }
}
