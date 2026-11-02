# Humanitarian Aid Distribution System

> Desktop application in **Java SE (Maven) + Swing** for planning the delivery of humanitarian aid to communities in need. It manages communities and vehicles (CRUD with an **H2** database through **JDBC**) and applies five classic **algorithm design techniques** — Divide and Conquer, Greedy, Dynamic Programming, Backtracking and Branch and Bound — plus sorting, searching, descriptive statistics and **Excel** reports.

Integrative project · Universidad Central del Ecuador

Spanish version: [README.es.md](README.es.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Algorithms](#algorithms)
4. [Project Requirements](#project-requirements)
5. [Installation and Execution](#installation-and-execution)
6. [Usage](#usage)
7. [Data Model and Validation](#data-model-and-validation)
8. [Database](#database)
9. [Reports and Excel Export](#reports-and-excel-export)
10. [Project Structure](#project-structure)
11. [Architecture](#architecture)

---

## Overview

The system helps decide **which communities to help first, how to share limited supplies, which communities fit in a shipment, in what order to visit them and which vehicle should serve each one**.

Each **community** has a name, number of inhabitants, a priority (1–10) and the resources it requires (tons). Its **benefit** is calculated as:

```
benefit = inhabitants × priority
```

Each **vehicle** has an identifier and a load capacity (kg).

The application starts with sample data (4 communities and 2 vehicles) so every feature can be tried right away.

## Features

- **Communities** — register, edit, delete, list and search.
- **Vehicles** — register, edit, delete, list and search.
- **Distribution** — five algorithm-design techniques applied to the registered data (see below).
- **Reports** — communities sorted by benefit, priority, required resources or name; vehicles sorted by capacity or identifier.
- **Statistics** — descriptive statistics and automatic conclusions.
- **Excel export** (`.xlsx`) with data sheets, live formulas, charts and conclusions.
- **Input validation** with clear messages (empty text, non-positive values, priority out of range, non-numeric input).
- **Splash screen** with loading progress while the database and sample data are prepared.
- **Binary search and sorting** used internally for lookups and listings.

## Algorithms

All algorithms are implemented from scratch in `dominio/Algoritmos.java`.

### Design techniques (Distribution tab)

| Technique | Problem solved | How it works |
|---|---|---|
| **Divide and Conquer** | Community with the **greatest need** | Recursively splits the list in halves and keeps the community with the highest benefit (`habitantes × prioridad`). |
| **Greedy** | **Supply distribution** | Sorts communities by priority (highest first) and gives each one `min(resources required, remaining supplies)` until the supplies run out. Reports the leftover. |
| **Dynamic Programming** | **Optimal selection of communities** (0/1 knapsack) | Chooses the set of communities that maximizes total benefit without exceeding the shipment capacity (tons); the weight of each community is its required resources. |
| **Backtracking** | **Optimal distribution route** (TSP type) | Finds the minimum-distance route that starts at the distribution center and visits every community, pruning branches that already exceed the best distance. Reports explored nodes and pruned branches. Limited to **8 communities** for efficiency. |
| **Branch and Bound** | **Assignment of vehicles to communities** | Assigns vehicles to communities maximizing coverage and minimizing wasted capacity (kg), pruning non-promising branches. Reports explored nodes and pruned branches. |

> In the route algorithm, distances between points are generated with the rule `10 + |i − j| × 8`.

### Sorting and searching

| Algorithm | Where it is used |
|---|---|
| **Quick sort** | Community benefit report; searches; default listings by priority / capacity. |
| **Merge sort** | Communities by priority. |
| **Insertion sort** | Communities by required resources. |
| **Bubble sort** | Communities by name. |
| **Selection sort** | Vehicles by capacity. |
| **Shell sort** | Vehicles by identifier. |
| **Sequential search** | Generic search by condition. |
| **Binary search (iterative and recursive)** | Finding a community or vehicle by identifier on a sorted list. |

## Project Requirements

### Software

| Requirement | Version | Notes |
|---|---|---|
| **JDK** | **17 or newer** | Configured in `pom.xml` (`maven.compiler.source/target = 17`). |
| **Apache Maven** | 3.6+ | Build and dependency management. Included in most IDEs. |
| **H2 Database** | 2.2.224 | Embedded SQL database (downloaded by Maven). |
| **Apache POI (poi-ooxml)** | 5.2.5 | Excel `.xlsx` generation (downloaded by Maven). |
| **Log4j Core** | 2.20.0 | Logging backend used by POI (avoids a StatusLogger warning). |
| **Swing** | Bundled with the JDK | Graphical interface. |

### Hardware / environment

- Any computer able to run Java 17 with a graphical desktop (Windows, macOS or Linux).
- No database server or internet connection is needed to run (internet is needed only to download dependencies the first time).

### Functional requirements

1. Manage communities and vehicles (create, read, update, delete) with persistence through JDBC.
2. Find the community with the greatest benefit (**Divide and Conquer**).
3. Distribute available supplies by priority (**Greedy**).
4. Select the best communities for a limited shipment (**Dynamic Programming**, 0/1 knapsack).
5. Compute the optimal distribution route (**Backtracking**).
6. Assign vehicles to communities (**Branch and Bound**).
7. Generate sorted reports and descriptive statistics.
8. Export the results to Excel.
9. Validate every user input.

### Non-functional requirements

- **OOP and MVC** — separate domain, persistence, GUI and reporting layers.
- **Maintainability** — one DAO per entity; business rules in manager classes.
- **Usability** — splash screen, clear messages and a tabbed interface.
- **Portability** — embedded database, no external services.

## Installation and Execution

```bash
# 1. Get the project
git clone <repository-url>
cd AyudaHumanitaria

# 2. Compile
mvn clean compile

# 3. Run
mvn exec:java

# (Optional) build the JAR
mvn clean package
```

Main class: `uce.edu.ec.ayudahumanitaria.ejecutable.Main`

You can also open the project in **IntelliJ IDEA** (or NetBeans / Eclipse) as a Maven project and run `Main`.

## Usage

The window has four tabs:

1. **Comunidades** — add, edit, delete and search communities.
2. **Vehículos** — add, edit, delete and search vehicles (capacity in kg).
3. **Distribución** — run the five distribution algorithms — community with the greatest need, supply distribution (enter available tons), optimal selection of communities (enter shipment capacity in tons), optimal route and vehicle assignment. Results appear in the *Resultados* panel.
4. **Reportes** — sorted reports of communities and vehicles, statistics with conclusions, and **Generar Excel**.

> The interface is in Spanish.

## Data Model and Validation

| Entity | Fields | Rules |
|---|---|---|
| **Community** | `id` (auto), `nombre`, `habitantes`, `prioridad`, `recursosRequeridos` (tons) | Name not empty · inhabitants > 0 · priority between 1 and 10 · resources > 0 |
| **Vehicle** | `id` (text), `capacidad` (kg), `asignado`, `comunidadAsignada` | Identifier not empty · capacity > 0 |

Sample data: Communities A–D (5,000 – 9,000 inhabitants, priorities 5–9) and vehicles V1 (1,000 kg) and V2 (1,500 kg).

## Database

The project uses **H2** through JDBC (`ConexionBD`). Two modes are available by editing the `URL` constant in `ConexionBD.java`:

| Mode | URL | Behavior |
|---|---|---|
| **In memory** (default) | `jdbc:h2:mem:ayudahumanitaria;DB_CLOSE_DELAY=-1` | Data is lost when the program closes; sample data is reloaded on every run. |
| **File** | `jdbc:h2:file:./data/ayudahumanitaria` | Data is kept between runs in `./data/ayudahumanitaria.mv.db`. |

Tables created automatically:

```sql
COMUNIDAD (ID INT AUTO_INCREMENT PRIMARY KEY, NOMBRE VARCHAR(100), HABITANTES INT,
           PRIORIDAD INT, RECURSOS_REQUERIDOS DOUBLE)
VEHICULO  (ID VARCHAR(20) PRIMARY KEY, CAPACIDAD DOUBLE, ASIGNADO BOOLEAN,
           COMUNIDAD_ASIGNADA VARCHAR(100))
```

All queries use `PreparedStatement`.

## Reports and Excel Export

**On-screen reports**

| Report | Sort algorithm |
|---|---|
| Communities by benefit | Quick sort |
| Communities by priority | Merge sort |
| Communities by required resources | Insertion sort |
| Communities by name | Bubble sort |
| Vehicles by capacity | Selection sort |
| Vehicles by identifier | Shell sort |

**Statistics** (`EstadisticaDescriptiva`, `AnalisisAyuda`) — mean, median, sample standard deviation, minimum, quartiles Q1/Q3, maximum, range, coefficient of variation and Pearson correlation (inhabitants vs. resources), for inhabitants, priority, resources, benefit and vehicle capacity; total benefit, total required resources vs. total fleet capacity, coverage percentage, deficit, assigned vehicles and high-priority communities (priority ≥ 8). The app also writes automatic conclusions.

**Excel file** (`AyudaHumanitaria_<timestamp>.xlsx`) with five sheets: *Resumen* (with charts of benefit and required resources per community), *Comunidades*, *Vehiculos*, *Estadisticas* (live formulas) and *Conclusiones*.

## Project Structure

```
AyudaHumanitaria/
├── pom.xml                                   Maven configuration
└── src/main/java/uce/edu/ec/ayudahumanitaria/
    ├── ejecutable/
    │   └── Main.java                         Entry point (splash + initialization)
    ├── dominio/                              Business logic (model)
    │   ├── Comunidad.java
    │   ├── Vehiculo.java
    │   ├── GestorComunidades.java            Community rules + DAO
    │   ├── GestorVehiculos.java              Vehicle rules + DAO
    │   ├── Algoritmos.java                   Sorting, searching and design techniques
    │   ├── EstadisticaDescriptiva.java       Descriptive statistics
    │   ├── AnalisisAyuda.java                Analysis and conclusions
    │   └── DatosEjemplo.java                 Sample data
    ├── persistencia/                         JDBC / H2
    │   ├── ConexionBD.java
    │   ├── ComunidadDAO.java
    │   ├── VehiculoDAO.java
    │   └── PersistenciaException.java
    ├── gui/                                  Swing interface (view / controller)
    │   ├── VentanaPrincipal.java
    │   ├── PanelComunidades.java
    │   ├── PanelVehiculos.java
    │   ├── PanelAlgoritmos.java
    │   ├── PanelReportes.java
    │   ├── SplashScreen.java
    │   └── EstiloUI.java
    ├── reportes/
    │   └── ExportadorExcel.java              Excel export with Apache POI
    └── util/
        ├── Validador.java
        └── ValidationException.java
```

## Architecture

The project follows **MVC with a persistence layer**:

- **Model** (`dominio`) — entities, manager classes with the business rules and validation, and the algorithms. It does not know the interface.
- **Persistence** (`persistencia`) — one DAO per entity; the only place that touches SQL.
- **View / Controller** (`gui`) — Swing panels that call the managers and show the results.
- **Reports** (`reportes`) — builds the Excel workbook from the model data.
- **Utilities** (`util`) — validation and custom exceptions (`ValidationException`, `PersistenciaException`) that carry user-friendly messages to the interface.

Startup flow: `Main` shows the splash screen → initializes the database → loads sample data if the tables are empty → opens `VentanaPrincipal`. The connection is closed automatically on exit.
