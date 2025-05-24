// Nunca cambia la declaracion del package!
package cc.carretera;

import es.upm.aedlib.Entry;
import es.upm.aedlib.indexedlist.*;
import es.upm.babel.cclib.Monitor;
import es.upm.aedlib.map.*;
import es.upm.aedlib.Pair;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Implementación del recurso compartido Carretera con Monitores
 */
public class CarreteraMonitor implements Carretera {

  private final Monitor mutex;

  /**
   * Array de Condiciones x Segmento <p>
   * if (!carrilLibre in SegmentoN) ==> segmentoLibre[SegmentoN].awaits(); (espera carril libre )
   */
  private final Monitor.Cond[] segmentoLibre;

  /**
   * Mapa <'id', Coche<'Pos(segmentos, carriles)', tks>> <p>
   * Guardar qué y dónde está cada coche
   */
  private final Map<String, Pair<Pos, Integer>> cr;

  /**
   * Número de segmentos
   */
  private final int segmentos;

  /**
   * Número de carriles
   */
  private final int carriles;

  /**
   * Lista<{0} ∪ {1...Segmentos - 1} <{1...Carriles}>> <p>
   *
   * Queue.length() = [0 (no hay carrilesLibres), this.carriles] <p>
   * Se usa Lista a modo de Iterador[this.segmentos]
   */
  private final IndexedList<Queue<Integer>> carrilesLibres;

  /**
   * Mapa<'id', Cond()> <p>
   * Mapa: qué coche y su condición
   */
  private final Map<String, Monitor.Cond> condPorCoche;

  public CarreteraMonitor(int segmentos, int carriles) {
    this.segmentos = segmentos;
    this.carriles = carriles;
    this.cr = new HashTableMap<>();
    this.mutex = new Monitor();
    this.condPorCoche = new HashTableMap<>();

    this.carrilesLibres = new ArrayIndexedList<>();
    //segmentos de {0 ... segmentos - 1}
    for (int i = 0; i < this.segmentos; i++){
      carrilesLibres.add(i, new LinkedList<>());
      for (int j = 1; j <= this.carriles; j++){ //carriles de {1 ... carriles}
        carrilesLibres.get(i).add(j);
      }
    }

    this.segmentoLibre = new Monitor.Cond[this.segmentos];
    //Cond() x Segmento
    for (int i = 0; i < this.segmentos; i++){
      segmentoLibre[i] = mutex.newCond();
    }
  }

  public Pos entrar(String id, int tks) {
    mutex.enter();

    //CPRE
    if (carrilesLibres.get(0).isEmpty()){ //if !carrilLibre in Segmento[0]
      segmentoLibre[0].await();
    } //CPRE

    //<código establece la post>
    // Se añade coche al mapa de coches y se crea Cond() para ESE coche
    cr.put(id, new Pair<>(new Pos(0 + 1, carrilesLibres.get(0).poll()), tks)); //0+1 informativo de cómo gestionar los segmentos
    condPorCoche.put(id, mutex.newCond());

    mutex.leave();
    return cr.get(id).getLeft();
  }

  public Pos avanzar(String id, int tks) {
    mutex.enter();

    // Se guarda segmentoActual y sigSegmento para mayor legibilidad
    int carrilActual = cr.get(id).getLeft().getCarril();
    int segmentoActual = cr.get(id).getLeft().getSegmento();
    int sigSegmento = segmentoActual + 1;

    //CPRE
    if (carrilesLibres.get(sigSegmento - 1).isEmpty()) { //if !carrilLibre in Segmento[sigSegmento - 1]
                                                          // sigSegmento - 1 | informativo de cómo gestionar los segmentos
      segmentoLibre[sigSegmento - 1].await();
    } //CPRE

    //<código que establece la post>
    int nuevoCarril = carrilesLibres.get(sigSegmento - 1).poll();

    // Se cambia info coche del mapa de coches y se añade el carrilLibre a segmentoActual - 1 ({0...Segmentos - 1}) y signal()
    cr.put(id, new Pair<>(new Pos(sigSegmento, nuevoCarril), tks));
    carrilesLibres.get(segmentoActual - 1).add(carrilActual);
    segmentoLibre[segmentoActual - 1].signal();

    mutex.leave();
    return cr.get(id).getLeft();
  }

  public void circulando(String id) {
    mutex.enter();
    if (cr.get(id).getRight() > 0){ //if t > 0 ==> Se bloquea el coche
      condPorCoche.get(id).await();
    }
    mutex.leave();
  }

  public void salir(String id) {
    mutex.enter();

    // Se guarda qué carril queda Libre
    int carrilLibre = cr.get(id).getLeft().getCarril();

    //Se elimina el coche del Mapa de coches y su Cond()
    cr.remove(id);
    condPorCoche.remove(id);

    //Se añade el carrilLibre a ESE segmento - 1 ({0...Segmentos - 1}) y signal()
    carrilesLibres.get(segmentos - 1).add(carrilLibre);
    segmentoLibre[segmentos - 1].signal();

    mutex.leave();
  }

  public void tick() {
    //Se decrementa t de todos los coches del Mapa de coches (se cambia su t por t - 1)
    Pair<Pos, Integer> parActual;
    for (Entry<String, Pair<Pos, Integer>> cocheActual : cr){
      parActual = cocheActual.getValue();
      cr.put(cocheActual.getKey(),
              new Pair<>(new Pos(parActual.getLeft().getSegmento(), parActual.getLeft().getCarril()),
                      parActual.getRight() - 1));

      if (cr.get(cocheActual.getKey()).getRight() <= 0) { //if t <= 0, se hace signal a su Cond() (bloqueado en circulando())
        mutex.enter();
        condPorCoche.get(cocheActual.getKey()).signal();
        mutex.leave();
      }
    }
  }
}
