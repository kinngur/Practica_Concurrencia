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
     * <p>
     * Queue.length() = [0 (no hay carrilesLibres), this.carriles] <p>
     * Se usa Lista a modo de Iterador[this.segmentos]
     */
    private final IndexedList<Queue<Integer>> carrilesLibres;

    private Any2OneChannel c_entrar, c_avanzar, c_circulando, c_salir, c_tick;

    private Collection<PetPeticiones> peticiones;

    /**
     * Peticiones a revisar
     */
    class PetPeticiones{
        PetCoche p;
        private ChannelOutput resp;
        private int numAcc;

        //clase PeticionesCoche(general) para gestionar las peticiones
        //queda comprobar si la idea de la implementacion del switch es la correcta o no
        //peticiones apalazadas es la idea inicial
        public PetPeticiones(PetCoche p, int num) {
            this.p = p;
            resp = this.p.c;
            numAcc = num;
        }
    }

    /**
     * Peticiones normales Coche
     */
    class PetCoche{
        /**
         * if (tks == -1) ==> tks not meant to be used
         * else ==> tks value meant to be used
         */
        private int tks;

        private String id;

        private ChannelOutput c;

        public PetCoche(String id, ChannelOutput c){
            this.id = id;
            this.c = c;
            this.tks = -1;
        }

        public PetCoche(String id, ChannelOutput c, int tks){
            this.id = id;
            this.c = c;
            this.tks = tks;
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

        this.peticiones = new ArrayList<>();

        // TODO: Creación de canales para comunicación con el servidor
        // Ej. chOp = Channel.any2one();

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
        // TODO: código que ejecuta el cliente para enviar/recibir un
        // mensaje al server para que ejecute entrar
        One2OneChannel c_resp = Channel.one2one();

        PetCoche pet = new PetCoche(car, c_resp.out(), tks);
        c_entrar.out().write(pet);
        Pos posAsignada = (Pos) c_resp.in().read();

        return posAsignada;
    }

    private boolean ejecutar_entrar(PetCoche pet) {
        boolean hecho = false;

        /*//CPRE
        //if carrilLibre in Segmento[0]
        if (!carrilesLibres.get(0).isEmpty()) {
            cr.put(pet.datos.getLeft(),
                    new Pair<>(new Pos(0 + 1, carrilesLibres.get(0).poll()), pet.datos.getRight()));
            pet.pos = cr.get(pet.datos.getLeft()).getLeft();
            hecho = true;
        } //CPRE*/

        //CPRE
        //if carrilLibre in Segmento[0]
        if (!carrilesLibres.get(0).isEmpty()){
            cr.put(pet.id,
                    new Pair<>(new Pos(0 + 1, carrilesLibres.get(0).poll()), pet.tks));
            hecho = true;
        }//CPRE

        return hecho;
    }

    public Pos avanzar(String car, int tks) {
        // TODO: código que ejecuta el cliente para enviar/recibir un
        // mensaje al server para que ejecute avanzar
        One2OneChannel c_resp = Channel.one2one();

        PetCoche pet = new PetCoche(car, c_resp.out(), tks);
        c_avanzar.out().write(pet);
        Pos posAsignada = (Pos) c_resp.in().read();

        return posAsignada;
    }

    private boolean ejecutar_avanzar(PetCoche pet){
        boolean hecho = false;

        // Se guarda segmentoActual y sigSegmento para mayor legibilidad
        int carrilActual = cr.get(pet.id).getLeft().getCarril();
        int segmentoActual = cr.get(pet.id).getLeft().getSegmento();
        int sigSegmento = segmentoActual + 1;

        //CPRE
        //if !carrilLibre in Segmento[sigSegmento - 1]
        if (!carrilesLibres.get(sigSegmento - 1).isEmpty()){
            int nuevoCarril = carrilesLibres.get(sigSegmento - 1).poll();

            // Se cambia info coche del mapa de coches y se añade el carrilLibre
            // a segmentoActual - 1 ({0...Segmentos - 1}) y signal()
            cr.put(pet.id,
                    new Pair<>(new Pos(sigSegmento, nuevoCarril), pet.tks));
            carrilesLibres.get(segmentoActual - 1).add(carrilActual);
            //segmentoLibre[segmentoActual - 1].signal();

            hecho = true;
        }//CPRE

        return hecho;
    }

    public void circulando(String car) {
        // TODO: código que ejecuta el cliente para enviar un mensaje al
        // server para que ejecute circulando
        One2OneChannel c_resp = Channel.one2one();

        PetCoche pet = new PetCoche(car, c_resp.out());
        c_circulando.out().write(pet);
        c_resp.in().read();
    }

    private boolean ejecutar_circulando(PetCoche pet){
        boolean hecho = false;

        //CPRE
        if (cr.get(pet.id).getRight() <= 0){
            hecho = true;
        }//CPRE

        return hecho;
    }

    public void salir(String car) {
        // TODO: código que ejecuta el cliente para enviar un mensaje al
        // server para que ejecute salir
        One2OneChannel c_resp = Channel.one2one();

        PetCoche pet = new PetCoche(car, c_resp.out());
        c_salir.out().write(pet);
        c_resp.in().read();
    }

    private void ejecutar_salir(PetCoche pet){
        // Se guarda qué carril queda Libre
        int carrilLibre = cr.get(pet.id).getLeft().getCarril();

        //Se elimina el coche del Mapa de coches y su Cond()
        cr.remove(pet.id);

        //Se añade el carrilLibre a ESTE segmento - 1 ({0...Segmentos - 1}) y signal()
        carrilesLibres.get(segmentos - 1).add(carrilLibre);
    }

    public void tick() {
        // TODO: código que ejecuta el cliente para enviar un mensaje al
        // server para que ejecute tick
        One2OneChannel c_resp = Channel.one2one();

        PetCoche pet = new PetCoche(null, c_resp.out());
        c_tick.out().write(pet);
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
        // TODO: declaración e inicialización del estado del recurso
        final int ENTRAR = 0;
        final int AVANZAR = 1;
        final int CIRCULANDO = 2;
        final int SALIR = 3;
        final int TICK = 4;

        //boolean[] sincCond = {true, true, true, true, true};

        AltingChannelInput[] entradas = {c_entrar.in(), c_avanzar.in(),
                c_circulando.in(), c_salir.in(), c_tick.in()};

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
            servicio = servicios.fairSelect();

            // TODO: ejecutar la operación solicitada por el cliente
            switch (servicio) {
                case ENTRAR:
                    PetCoche arg = (PetCoche) c_entrar.in().read();
                    res = ejecutar_entrar(arg);
                    if (res) {
                        arg.c.write(cr.get(arg.id).getLeft());
                    } else {
                        PetPeticiones p = new PetPeticiones(arg, 1);
                        peticiones.add(p);
                    }
                    break;
                case AVANZAR:
                    PetCoche arg2 = (PetCoche) c_avanzar.in().read();
                    res = ejecutar_avanzar(arg2);
                    if (res) {
                        arg2.c.write(cr.get(arg2.id).getLeft());
                    } else {
                        PetPeticiones p = new PetPeticiones(arg2, 2);
                        peticiones.add(p);
                    }
                    break;
                case CIRCULANDO:
                    PetCoche arg3 = (PetCoche) c_circulando.in().read();
                    res = ejecutar_circulando(arg3);
                    if (res) {
                        arg3.c.write("");
                    } else {
                        PetPeticiones p = new PetPeticiones(arg3, 3);
                        peticiones.add(p);
                    }
                    break;
                case SALIR:
                    PetCoche arg4 = (PetCoche) c_salir.in().read();
                    ejecutar_salir(arg4);
                    arg4.c.write("");
                    break;
                case TICK:
                    PetCoche arg5 = (PetCoche) c_tick.in().read();
                    ejecutar_tick();
                    arg5.c.write("");
                    break;
            }
            // TODO: atender peticiones pendientes que puedan ser atendidas
            RevisarPeticiones();
        }
    }

    private void RevisarPeticiones() {
        Iterator<PetPeticiones> it = peticiones.iterator();
        //Object res;
        boolean hayAplazadas;
        do {
            hayAplazadas = false;
            while (it.hasNext()) {
                PetPeticiones p = it.next();
                boolean realizado = false;
                switch (p.numAcc) {
                    case 1: realizado = ejecutar_entrar(p.p); break;
                    case 2: realizado = ejecutar_avanzar(p.p); break;
                    case 3: realizado = ejecutar_circulando(p.p); break;
                }
                if (realizado) {
                    p.resp.write(cr.get(p.p.id).getLeft());
                    hayAplazadas = true;
                    it.remove();
                }
            }
        } while (hayAplazadas);
    }
}