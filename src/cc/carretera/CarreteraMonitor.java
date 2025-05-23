// Nunca cambia la declaracion del package!
package cc.carretera;

import es.upm.aedlib.Entry;
import es.upm.babel.cclib.Monitor;
import es.upm.aedlib.map.*;
import es.upm.aedlib.Pair;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Implementación del recurso compartido Carretera con Monitores
 */
public class CarreteraMonitor implements Carretera {
  // TODO: añadir atributos para representar el estado del recurso y
  // la gestión de la concurrencia (monitor y conditions)

  private Monitor mutex;

//  private Monitor.Cond[] carrilesLibres;

//  private Monitor.Cond[] segmentosLibres;

  /**
   * carreteraLibre[nº segmentos][nº carriles]
   * <p>
   * for [nº segmentos] there exists [nº carriles] <p>
   * we check [i]segmento --> [0...j][carriles]
   */
  private Monitor.Cond[] carreteraLibre;

  private boolean[][] isCarreteraNotLibre; //aprovechando que se inicia a false

  private volatile Map<String, Pair<Pos, Integer>> cr;

  private int segmentos, carriles;

  private Queue<Integer>[] carrilesLibres;

  private Map<String, Monitor.Cond> condPorCoche;

  @SuppressWarnings("unchecked")
  public CarreteraMonitor(int segmentos, int carriles) {
    this.segmentos = segmentos;
    this.carriles = carriles;
    this.cr = new HashTableMap<>();
    this.mutex = new Monitor();

    this.carrilesLibres = (Queue<Integer>[]) new LinkedList[segmentos];

    for (int i = 0; i < segmentos; i++){
      carrilesLibres[i] = new LinkedList<>();
      for (int j = 0; j < carriles; j++){
        carrilesLibres[i].add(j);
      }
    }

    for (int i = 0; i < segmentos; i++){
      carreteraLibre[i] = mutex.newCond();
    }

    this.condPorCoche = new HashTableMap<>();
  }

  public Pos entrar(String id, int tks) {
    /*
    if (cr.containsKey(id)) {
      throw new IllegalAccessException("");
    }*/
    mutex.enter();

    //CPRE
    if (carrilesLibres[0].peek() == null){ //!CPRE
      carreteraLibre[0].await();
    } //CPRE

    //<código establece la post>
    cr.put(id, new Pair<>(new Pos(1, carrilesLibres[0].poll()), tks));
    condPorCoche.put(id, mutex.newCond());

    mutex.leave();
    return cr.get(id).getLeft();
  }

  public Pos avanzar(String id, int tks) {
    mutex.enter();

    //CPRE
    int nuevoSegmento = cr.get(id).getLeft().getSegmento() + 1;
    if (carrilesLibres[nuevoSegmento].peek() == null) { //!CPRE
      carreteraLibre[nuevoSegmento].await();
    } //CPRE

    //<código que establece la post>
    int carrilLibre = cr.get(id).getLeft().getCarril();
    cr.put(id, new Pair<>(new Pos(nuevoSegmento, carrilesLibres[nuevoSegmento].poll()), tks));
    carrilesLibres[nuevoSegmento - 1].add(carrilLibre);
    carreteraLibre[nuevoSegmento - 1].signal();

    mutex.leave();
    return cr.get(id).getLeft();
  }

  public void circulando(String id) {
    mutex.enter();
    if (cr.get(id).getRight() != 0){
      condPorCoche.get(id).await();
    }
    mutex.leave();
  }

  public void salir(String id) {
    int carrilLibre =cr.get(id).getLeft().getCarril();
    cr.remove(id);
    condPorCoche.remove(id);
    carrilesLibres[segmentos].add(carrilLibre);
    carreteraLibre[segmentos].signal();
  }

  public void tick() {
    for (Entry<String, Pair<Pos, Integer>> coche : cr){
      cr.put(coche.getKey(), new Pair<>(coche.getValue().getLeft(), coche.getValue().getRight() - 1));
      if (cr.get(coche.getKey()).getRight() == 0){
        condPorCoche.get(coche.getKey()).signal();
      }
    }
  }
}
