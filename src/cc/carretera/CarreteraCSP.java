package cc.carretera;

import es.upm.aedlib.Pair;
import es.upm.aedlib.indexedlist.ArrayIndexedList;
import es.upm.aedlib.indexedlist.IndexedList;
import es.upm.aedlib.map.HashTableMap;
import es.upm.aedlib.map.Map;
import org.jcsp.lang.*;

import java.util.*;

public class CarreteraCSP implements Carretera, CSProcess {
  // TODO: Declaración de canales
  // Ej. private Any2One chOp;

  /**
   * Número de segmentos
   */
  private final int segmentos;

  /**
   * Número de carriles
   */
  private final int carriles;

  /**
   * Mapa <'id', Coche<'Pos(segmentos, carriles)', tks>> <p>
   * Guardar qué y dónde está cada coche
   */
  private final Map<String, Pair<Pos, Integer>> cr;

  /**
   * Lista<{0} ∪ {1...Segmentos - 1} <{1...Carriles}>> <p>
   *
   * Queue.length() = [0 (no hay carrilesLibres), this.carriles] <p>
   * Se usa Lista a modo de Iterador[this.segmentos]
   */
  private final IndexedList<Queue<Integer>> carrilesLibres;

  private Any2OneChannel c_entrar, c_avanzar, c_circulando, c_salir;

  private Collection<PetCoche> peticiones;

  class PetEntrar {
    Pair<String, Integer> datos;
    ChannelOutput ac;
    Pos pos;

    public PetEntrar(Pair<String, Integer> datos, ChannelOutput ac){
      this.datos = datos;
      this.ac = ac;
      this.pos = null;
    }
  }

  class PetCoche {

    //clase PeticionesCoche(general) para gestionar las peticiones
    //queda comprobar si la idea de la implementacion del switch es la correcta o no
    //peticiones apalazadas es la idea inicial
    public PetCoche(){

    }
  }

  public CarreteraCSP(int segmentos, int carriles) {
    this.segmentos = segmentos;
    this.carriles = carriles;

    this.cr = new HashTableMap<>();

    this.carrilesLibres = new ArrayIndexedList<>();
    //segmentos de {0 ... segmentos - 1}
    for (int i = 0; i < this.segmentos; i++){
      carrilesLibres.add(i, new LinkedList<>());

      //carriles de {1 ... carriles}
      for (int j = 1; j <= this.carriles; j++){
        carrilesLibres.get(i).add(j);
      }
    }

    this.peticiones = new ArrayList<PetCoche>();

    // TODO: Creación de canales para comunicación con el servidor
    // Ej. chOp = Channel.any2one();

    this.c_entrar = Channel.any2one();
    this.c_avanzar = Channel.any2one();
    this.c_circulando = Channel.any2one();
    this.c_salir = Channel.any2one();

    // Puesta en marcha del servidor: alternativa sucia (desde el
    // punto de vista de CSP) a Parallel que nos ofrece JCSP para
    // poner en marcha un CSProcess
    new ProcessManager(this).start();
  }

  public Pos entrar(String car, int tks) {
    // TODO: código que ejecuta el cliente para enviar/recibir un
    // mensaje al server para que ejecute entrar
    One2OneChannel c_resp = Channel.one2one();

    PetEntrar pet = new PetEntrar(new Pair<>(car, tks), c_resp.out());
    c_entrar.out().write(pet);
    Pos posicionAsignada = (Pos) c_resp.in().read();

    return posicionAsignada;
  }

  private boolean ejecutar_entrar(PetEntrar pet){
    boolean hecho = false;

    //CPRE
    //if carrilLibre in Segmento[0]
    if (!carrilesLibres.get(0).isEmpty()){
      cr.put(pet.datos.getLeft(),
              new Pair<>(new Pos(0 + 1, carrilesLibres.get(0).poll()), pet.datos.getRight()));
      pet.pos = cr.get(pet.datos.getLeft()).getLeft();
      hecho = true;
    } //CPRE
    return hecho;
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
    final int ENTRAR = 0; final int AVANZAR = 1; final int CIRCULANDO = 2;
    final int SALIR = 3;
    boolean[] sincCond = new boolean[]{};
    AltingChannelOutput[] entradas = new AltingChannelOutput[4];
    boolean res;

    // TODO: declaración e inicialización de estructuras de datos para
    // almacenar peticiones de los clientes

    // TODO: declaración e inicialización de arrays necesarios para
    // poder hacer la recepción no determinista (Alternative)

    // TODO: cambiar null por el array de canales
    Alternative servicios = new Alternative(entradas);

    // Bucle principal del servicio
    while(true){
      // TODO: declaración de variables auxiliares
      int servicio;

      // TODO: cálculo de las guardas

      // TODO: cambiar null por el array de guardas
      servicio = servicios.fairSelect(sincCond);

      // TODO: ejecutar la operación solicitada por el cliente
      switch (servicio){
      case ENTRAR:
        // TODO: ejecutar operación 0 o almacenar la petición y
        // responder al cliente si es posible
        PetEntrar arg = (PetEntrar) c_entrar.in().read();
        res = ejecutar_entrar(arg);
        if (res) {
          arg.ac.write(arg.pos);
        } else {
          PetCoche p = new PetCoche();
          peticiones.add(p);
        }
        break;
      }
      // TODO: atender peticiones pendientes que puedan ser atendidas
      RevisarPeticiones();
    }
  }

  private void RevisarPeticiones(){
    Iterator<PetCoche> it = peticiones.iterator();
    Object res; boolean hayAplazadas;
    do {
      hayAplazadas = false;
      while (it.hasNext()){

      }
    } while (hayAplazadas);
  }

}
