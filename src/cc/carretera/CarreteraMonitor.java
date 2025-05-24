// Nunca cambia la declaracion del package!
package cc.carretera;

import es.upm.aedlib.Entry;
import es.upm.aedlib.indexedlist.ArrayIndexedList;
import es.upm.aedlib.indexedlist.IndexedList;
import es.upm.babel.cclib.Monitor;
import es.upm.aedlib.map.*;
import es.upm.aedlib.Pair;

import java.util.LinkedList;
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
   * Array de Condiciones x Segmento <p>
   * if (!carrilLibre in Segmento) ==> segmentoLibre[Segmento].awaits(); (espera carril libre)
   * Cuando carrilLibre in Segmento ==> signal();
   */
  private Monitor.Cond[] segmentoLibre;
  //TODO mejor nombre

  /**
   * carreteraLibre[nº segmentos][nº carriles]
   * <p>
   * for [nº segmentos] there exists [nº carriles] <p>
   * we check [i]segmento --> [0...j][carriles]
   */
  //private boolean[][] isCarreteraNotLibre; //aprovechando que se inicia a false

  /**
   * Mapa <'id', Coche<'Pos(segmentos, carriles)', tks>> <p>
   * Guardar qué y dónde está cada coche
   */
  private volatile Map<String, Pair<Pos, Integer>> cr;

  /**
   * Número de segmentos
   */
  private int segmentos;

  /**
   * Número de carriles
   */
  private int carriles;

  /**
   * Lista<{0} ∪ {1...Segmentos - 1} <{1...Carriles}>> <p>
   * Queue.length() = [0 (no hay carrilesLibres), this.carriles]
   * Se usa Lista a modo de Iterador[this.segmentos]
   * Segmento que retorna Queue se le suma 1 para eliminar el {0}
   */
  private IndexedList<Queue<Integer>> carrilesLibres;

  /**
   * Mapa<'id', Cond()> <p>
   * Mapa: qué coche y su condición
   */
  private Map<String, Monitor.Cond> condPorCoche;

  public CarreteraMonitor(int segmentos, int carriles) {
    this.segmentos = segmentos;
    this.carriles = carriles;
    this.cr = new HashTableMap<>();
    this.mutex = new Monitor();

    this.carrilesLibres = new ArrayIndexedList<>();

    for (int i = 0; i < this.segmentos; i++){
      carrilesLibres.add(i, new LinkedList<>());
      for (int j = 1; j <= this.carriles; j++){
        carrilesLibres.get(i).add(j);
      }
    }

    this.segmentoLibre = new Monitor.Cond[this.segmentos];

    for (int i = 0; i < this.segmentos; i++){
      segmentoLibre[i] = mutex.newCond();
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
    if (carrilesLibres.get(0).peek() == null){ //!CPRE
      segmentoLibre[0].await();
    } //CPRE

    //<código establece la post>
    cr.put(id, new Pair<>(new Pos(0 + 1, carrilesLibres.get(0).poll()), tks));
    condPorCoche.put(id, mutex.newCond());

    mutex.leave();
    return cr.get(id).getLeft();
  }

  public Pos avanzar(String id, int tks) {
    mutex.enter();

    Pair<Pos, Integer> current = cr.get(id);
    Pos currentPos = current.getLeft();
    int currentSegment = currentPos.getSegmento();
    int currentCarril = currentPos.getCarril();
    int sigSegmento = currentSegment + 1;

    //CPRE
//    int sigSegmento = cr.get(id).getLeft().getSegmento() + 1;
    if (carrilesLibres.get(sigSegmento - 1).isEmpty()) { //!CPRE
      segmentoLibre[sigSegmento - 1].await();
    } //CPRE

    //<código que establece la post>
    int nuevoCarril = carrilesLibres.get(sigSegmento - 1).poll();

//    int carrilLibre = cr.get(id).getLeft().getCarril();
    cr.put(id, new Pair<>(new Pos(sigSegmento, nuevoCarril), tks));
    carrilesLibres.get(currentSegment - 1).add(currentCarril);
    segmentoLibre[currentSegment - 1].signal();

    mutex.leave();
    return cr.get(id).getLeft();
  }

  public void circulando(String id) {
    mutex.enter();
    if (cr.get(id).getRight() > 0){
      condPorCoche.get(id).await();
    }
    mutex.leave();
  }

  public void salir(String id) {
    mutex.enter();
    int carrilLibre = cr.get(id).getLeft().getCarril();
    cr.remove(id);
    condPorCoche.remove(id);
    carrilesLibres.get(segmentos - 1).add(carrilLibre);
    segmentoLibre[segmentos - 1].signal();
    mutex.leave();
  }

  public void tick() {
//    mutex.enter();
    for (Entry<String, Pair<Pos, Integer>> coche : cr){
      cr.put(coche.getKey(), new Pair<>(
              new Pos(coche.getValue().getLeft().getSegmento(), coche.getValue().getLeft().getCarril())
              , coche.getValue().getRight() - 1));
      if (cr.get(coche.getKey()).getRight() <= 0) {
        mutex.enter();
        condPorCoche.get(coche.getKey()).signal();
        mutex.leave();
      }
    }
//    mutex.leave();
  }
}
