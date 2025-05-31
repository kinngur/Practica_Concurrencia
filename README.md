Práctica Concurrencia
=====================================
---

Este proyecto forma parte de la asignatura **Concurrencia** del Grado en Ingeniería Informática en la UPM.
Su objetivo es simular una carretera con múltiples vehículos usando distintos mecanismos de sincronización concurrente.

---

## Project structure

~~~text
Practica_Concurrencia
├── lib
│   └── jcsp.jar # Biblioteca de concurrencia JCSP
└── src
    └── cc
        └── carretera
            └── Carretera.java
            └── CarreteraCSP.java
            └── CarreteraMonitor.java
            └── Coche.java
            └── Pos.java
            └── Reloj.java
            
~~~

---

## Project description

- `Carretera.java`: Clase base que define la lógica general de la carretera.
- `CarreteraMonitor.java`: Implementación de la carretera usando monitores.
- `CarreteraCSP.java`: Versión basada en CSP (Communicating Sequential Processes), haciendo uso de la biblioteca **JCSP**.
- `CarreteraSim.java`: Clase para simular localmente.
- `Coche.java`: Representa un coche que circula por la carretera.
- `Pos.java`: Posición en la carretera.
- `Reloj.java`: Módulo de temporización para sincronizar el paso del tiempo.

---

## Project libraries

Este proyecto utiliza la biblioteca [JCSP](https://www.cs.kent.ac.uk/projects/ofa/jcsp/), distribuida bajo la licencia [LGPL v2.1](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html).  
Este proyecto también utiliza dos bibliotecas proporcionadas por la asignatura: `aedlib.jar` y `cclib-0.4.9.jar`. Estas librerías son de uso exclusivo para fines académicos en el contexto de la Universidad Politécnica de Madrid y no se incluyen en este repositorio por motivos de licencia.

---