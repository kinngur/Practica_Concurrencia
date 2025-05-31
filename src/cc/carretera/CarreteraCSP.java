package cc.carretera;

import es.upm.aedlib.Entry;
import es.upm.aedlib.Pair;
import es.upm.aedlib.indexedlist.ArrayIndexedList;
import es.upm.aedlib.indexedlist.IndexedList;
import es.upm.aedlib.map.HashTableMap;
import es.upm.aedlib.map.Map;
import org.jcsp.lang.*;
import java.util.*;

public class CarreteraCSP implements Carretera, CSProcess {

    /**
     * Número de segmentos
     */
    private final int segmentos;

    /**
     * Número de carriles
     */
    private final int carriles;

    /**
     * Mapa <'id', Coche<'Pos(segmentos, carriles)', tks>>
     * <p>
     * Guardar qué y dónde está cada coche
     */
    private final Map<String, Pair<Pos, Integer>> cr;

    /**
     * Lista<{0} ∪ {1...Segmentos - 1} <{1...Carriles}>>
     * <p>
     * Queue.length() = [0 (no hay carrilesLibres), this.carriles]
     * <p>
     * Se usa Lista a modo de Iterador[this.segmentos]
     */
    private final IndexedList<Queue<Integer>> carrilesLibres;

    /**
     * Canal
     * <p>
     * Se declara un canal por cada acción posible: c_nombreAcción
     */
    private final Any2OneChannel c_entrar, c_avanzar, c_circulando, c_salir, c_tick;

    /**
     * Colección de peticiones Aplazadas
     */
    private final Collection<PetAplazadas> petAplazadas;

    /**
     * Clase auxiliar: Peticiones Coche
     */
    class PetCoche{
        /** Id del Coche para ubicarlo en el mapa cr*/
        private final String id;

        /** El canal de salida*/
        private final ChannelOutput c;

        /** Número de tks*/
        private int tks;

        /** Constructor sin tks*/
        public PetCoche(String id, ChannelOutput c){
            this.id = id;
            this.c = c;
        }

        /** Constructor con tks*/
        public PetCoche(String id, ChannelOutput c, int tks){
            this.id = id;
            this.c = c;
            this.tks = tks;
        }
    }

    /**
     * Clase auxiliar: Peticiones a revisar
     */
    class PetAplazadas{
        /** PetCoche (id, canal, tks si necesario)*/
        PetCoche petCoche;

        /** Qué numeroAcción tiene asignado en peticionesAplazadas()*/
        private final int numAcc;

        public PetAplazadas(PetCoche petCoche, int num) {
            this.petCoche = petCoche;
            numAcc = num;
        }
    }

    public CarreteraCSP(int segmentos, int carriles) {
        this.segmentos = segmentos;
        this.carriles = carriles;

        this.cr = new HashTableMap<>();

        this.carrilesLibres = new ArrayIndexedList<>();
        //segmentos de {0 ... segmentos - 1}
        for (int i = 0; i < this.segmentos; i++) {
            carrilesLibres.add(i, new LinkedList<>());

            //carriles de {1 ... carriles}
            for (int j = 1; j <= this.carriles; j++) {
                carrilesLibres.get(i).add(j);
            }
        }

        this.petAplazadas = new ArrayList<>();


        this.c_entrar = Channel.any2one();
        this.c_avanzar = Channel.any2one();
        this.c_circulando = Channel.any2one();
        this.c_salir = Channel.any2one();
        this.c_tick = Channel.any2one();

        // Puesta en marcha del servidor: alternativa sucia (desde el
        // punto de vista de CSP) a Parallel que nos ofrece JCSP para
        // poner en marcha un CSProcess
        new ProcessManager(this).start();
    }


    public Pos entrar(String car, int tks) {
        //Procedimiento: Se pasa por canal 'entrar' petCoche(id, canalRespuesta, tks)
        One2OneChannel c_resp = Channel.one2one();

        PetCoche pet = new PetCoche(car, c_resp.out(), tks);
        c_entrar.out().write(pet);

        // Se lee Pos por canalRespuesta y se retorna
        Pos posAsignada = (Pos) c_resp.in().read();

        return posAsignada;
    }

    private boolean ejecutar_entrar(PetCoche pet) {
        boolean cumpleCPRE = false;

        //CPRE
        //if carrilLibre in Segmento[0]
        if (!carrilesLibres.get(0).isEmpty()){
            cr.put(pet.id,
                    new Pair<>(new Pos(0 + 1, carrilesLibres.get(0).poll()), pet.tks));
            cumpleCPRE = true;
        }//CPRE

        return cumpleCPRE;
    }

    public Pos avanzar(String car, int tks) {
        //Procedimiento: Se pasa por canal 'avanzar' petCoche(id, canalRespuesta, tks)
        One2OneChannel c_resp = Channel.one2one();

        PetCoche pet = new PetCoche(car, c_resp.out(), tks);
        c_avanzar.out().write(pet);

        // Se lee Pos por canalRespuesta y se retorna
        Pos posAsignada = (Pos) c_resp.in().read();

        return posAsignada;
    }

    private boolean ejecutar_avanzar(PetCoche pet){
        boolean cumpleCPRE = false;

        // Se guarda segmentoActual y sigSegmento para mayor legibilidad
        int carrilActual = cr.get(pet.id).getLeft().getCarril();
        int segmentoActual = cr.get(pet.id).getLeft().getSegmento();
        int sigSegmento = segmentoActual + 1;

        //CPRE
        //if !carrilLibre in Segmento[sigSegmento - 1]
        if (!carrilesLibres.get(sigSegmento - 1).isEmpty()){
            int nuevoCarril = carrilesLibres.get(sigSegmento - 1).poll();

            // Se cambia info coche del mapa de coches y se añade el carrilLibre
            // a segmentoActual - 1 ({0...Segmentos - 1})
            cr.put(pet.id,
                    new Pair<>(new Pos(sigSegmento, nuevoCarril), pet.tks));
            carrilesLibres.get(segmentoActual - 1).add(carrilActual);

            cumpleCPRE = true;
        }//CPRE

        return cumpleCPRE;
    }

    public void circulando(String car) {
        //Procedimiento: Se pasa por canal 'circulando' petCoche(id, canalRespuesta)
        One2OneChannel c_resp = Channel.one2one();

        PetCoche pet = new PetCoche(car, c_resp.out());
        c_circulando.out().write(pet);

        // Se lee Ack por canalRespuesta
        c_resp.in().read();
    }

    private boolean ejecutar_circulando(PetCoche pet){
        boolean cumpleCPRE = false;

        //CPRE
        if (cr.get(pet.id).getRight() <= 0){
            cumpleCPRE = true;
        }//CPRE

        return cumpleCPRE;
    }

    public void salir(String car) {
        //Procedimiento: Se pasa por canal 'salir' petCoche(id, canalRespuesta)
        One2OneChannel c_resp = Channel.one2one();

        PetCoche pet = new PetCoche(car, c_resp.out());
        c_salir.out().write(pet);

        // Se lee Ack por canalRespuesta
        c_resp.in().read();
    }

    private void ejecutar_salir(PetCoche pet){
        // Se guarda qué carril queda Libre
        int carrilLibre = cr.get(pet.id).getLeft().getCarril();

        //Se elimina el coche del Mapa de coches
        cr.remove(pet.id);

        //Se añade el carrilLibre a ESTE segmento - 1 ({0...Segmentos - 1})
        carrilesLibres.get(segmentos - 1).add(carrilLibre);
    }

    public void tick() {
        // Procedimiento: Se pasa por canal 'tick' petCoche(null, canalRespuesta)
        One2OneChannel c_resp = Channel.one2one();

        PetCoche pet = new PetCoche(null, c_resp.out());
        c_tick.out().write(pet);

        // Se lee Ack por canalRespuesta
        c_resp.in().read();
    }

    private void ejecutar_tick(){
        //Se decrementa t de todos los coches del Mapa de coches (se cambia su t por t - 1)
        Pair<Pos, Integer> parActual;
        Pos posActual;
        for (Entry<String, Pair<Pos, Integer>> cocheActual : cr){
            parActual = cocheActual.getValue();
            posActual = parActual.getLeft();
            cr.put(cocheActual.getKey(),
                    new Pair<>(new Pos(posActual.getSegmento(), posActual.getCarril()),
                            parActual.getRight() - 1)
            );
        }
    }

    // Código del servidor
    public void run() {
        // Se declaran condiciones del switch
        final int ENTRAR = 0;
        final int AVANZAR = 1;
        final int CIRCULANDO = 2;
        final int SALIR = 3;
        final int TICK = 4;

        //Se declaran canalesEntrada x acción y variables a usar en el while
        AltingChannelInput[] entradas = {c_entrar.in(), c_avanzar.in(),
                c_circulando.in(), c_salir.in(), c_tick.in()};
        boolean res; int servicio;

        Alternative servicios = new Alternative(entradas);

        // Bucle principal del servicio
        while(true){
            servicio = servicios.fairSelect();

            switch (servicio) {
                /*
                 * Caso Entrar (Bloqueante):
                 * Si se cumple la petición, se devuelve un Pos por canal
                 * Se añade a petAplazadas de lo contrario
                 */
                case ENTRAR:
                    PetCoche arg = (PetCoche) c_entrar.in().read();
                    res = ejecutar_entrar(arg);
                    if (res) {
                        arg.c.write(cr.get(arg.id).getLeft());
                    } else {
                        PetAplazadas p = new PetAplazadas(arg, 1);
                        petAplazadas.add(p);
                    }
                    break;
                /*
                 * Caso Avanzar (Bloqueante):
                 * Si se cumple la petición, se devuelve un Pos por canal
                 * Se añade a petAplazadas de lo contrario
                 */
                case AVANZAR:
                    PetCoche arg2 = (PetCoche) c_avanzar.in().read();
                    res = ejecutar_avanzar(arg2);
                    if (res) {
                        arg2.c.write(cr.get(arg2.id).getLeft());
                    } else {
                        PetAplazadas p = new PetAplazadas(arg2, 2);
                        petAplazadas.add(p);
                    }
                    break;
                /*
                 * Caso Circulando (Bloqueante):
                 * Si se cumple la petición, se devuelve un Ack cualquiera por Canal
                 * Se añade a petAplazadas de lo contrario
                 */
                case CIRCULANDO:
                    PetCoche arg3 = (PetCoche) c_circulando.in().read();
                    res = ejecutar_circulando(arg3);
                    if (res) {
                        arg3.c.write("");
                    } else {
                        PetAplazadas p = new PetAplazadas(arg3, 3);
                        petAplazadas.add(p);
                    }
                    break;
                /*
                 * Caso Salir (No Bloqueante):
                 * Se ejecuta y se devuelve un Ack cualquiera por Canal
                 */
                case SALIR:
                    PetCoche arg4 = (PetCoche) c_salir.in().read();
                    ejecutar_salir(arg4);
                    arg4.c.write("");
                    break;
                /*
                 * Caso Tick (No Bloqueante):
                 * Se ejecuta y se devuelve un Ack cualquiera por Canal
                 */
                case TICK:
                    PetCoche arg5 = (PetCoche) c_tick.in().read();
                    ejecutar_tick();
                    arg5.c.write("");
                    break;
            }
            // Se comprueba petAplazadas
            RevisarPeticiones();
        }
    }

    private void RevisarPeticiones() {
        // Se declaran variables a usar en el bucle
        Iterator<PetAplazadas> it; boolean hayAplazadas, realizado;
        PetAplazadas p;

        /* Procedimiento: Se comprueban peticiones Aplazadas.
         * Si alguna se cumple, se elimina y se comprueba el resto
         * por si alguna otra se cumpliese gracias a esta.
         */
        do {
            hayAplazadas = false;
            it = petAplazadas.iterator();
            while (it.hasNext()) {
                p = it.next();
                realizado = false;
                switch (p.numAcc) {
                    case 1: realizado = ejecutar_entrar(p.petCoche); break;
                    case 2: realizado = ejecutar_avanzar(p.petCoche); break;
                    case 3: realizado = ejecutar_circulando(p.petCoche); break;
                }
                if (realizado) {
                    // Como los canales necesitan Pos o Ack cualquiera,
                    // se devuelve siempre un Pos por Canal
                    p.petCoche.c.write(cr.get(p.petCoche.id).getLeft());
                    hayAplazadas = true;
                    it.remove();
                }
            }
        } while (hayAplazadas);
    }
}