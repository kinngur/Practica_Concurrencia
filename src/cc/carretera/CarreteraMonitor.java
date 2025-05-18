// Nunca cambia la declaracion del package!
package cc.carretera;

import es.upm.babel.cclib.Monitor;
import es.upm.aedlib.map.*;
import es.upm.aedlib.Pair;

/**
 * Implementación del recurso compartido Carretera con Monitores
 */
public class CarreteraMonitor implements Carretera {
  // TODO: añadir atributos para representar el estado del recurso y
  // la gestión de la concurrencia (monitor y conditions)

  private Monitor mutex;

  private Monitor.Cond carrilesLibres;

  private Map<String, Pair<Pos, Integer>> cr;

  private int segmentos, carriles;

  public CarreteraMonitor(int segmentos, int carriles) {
    this.segmentos = segmentos;
    this.carriles = carriles;
    this.cr = new HashTableMap<>();

    this.mutex = new Monitor();
    this.carrilesLibres = mutex.newCond();
  }

  public Pos entrar(String id, int tks) {
    if (cr.containsKey(id)) {
      throw new Exception("");
    }
    mutex.enter();
    // cr.put(id, new Pair<Pos, Integer>((this.segmentos, this.carriles), tks));
    cr.put(id, new Pair<Pos, Integer>(new Pos(this.segmentos, this.carriles), tks));
    mutex.leave();
    return cr.get(id).getLeft();
  }

  public Pos avanzar(String id, int tks) {
    // TODO: implementar avanzar
    return null;
  }

  public void circulando(String id) {
    // TODO: implementar circulando
  }

  public void salir(String id) {
    // TODO: implementar salir
  }

  public void tick() {
    // TODO: implementar tick
  }
}
