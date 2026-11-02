# 🚚 Humanitarian Aid Distribution System

**An intelligent decision-support desktop application for planning humanitarian aid: who to help first, how to share limited supplies, which communities fit in a shipment, in what order to visit them and which vehicle serves each one.**

*Sistema Inteligente de Distribución de Ayuda Humanitaria* is a **Java SE (Maven) + Swing** application created as an **integrative project** at the Universidad Central del Ecuador. It manages **communities** and **vehicles** (full CRUD with an **H2** database through **JDBC**) and solves the planning problems with **five classic algorithm-design techniques** — Divide and Conquer, Greedy, Dynamic Programming, Backtracking and Branch and Bound — plus hand-written sorting and searching algorithms, descriptive statistics with automatic conclusions, and a formatted **Excel** report.

> The app starts with sample data (4 communities and 2 vehicles), so every feature can be tried immediately.

---

## Table of Contents

1. [Features at a Glance](#-features-at-a-glance)
2. [Project Requirements](#-project-requirements)
3. [Installation](#-installation)
4. [Running the Application](#-running-the-application)
5. [How to Use It (Step by Step)](#-how-to-use-it-step-by-step)
6. [The Problem Model](#-the-problem-model)
7. [The Five Algorithm-Design Techniques](#-the-five-algorithm-design-techniques)
8. [Sorting and Searching Algorithms](#-sorting-and-searching-algorithms)
9. [Reports, Statistics and Excel Export](#-reports-statistics-and-excel-export)
10. [Database](#-database)
11. [Validation Rules](#-validation-rules)
12. [Project Structure](#-project-structure)
13. [Architecture and Design Notes](#-architecture-and-design-notes)
14. [Assumptions and Limitations](#-assumptions-and-limitations)
15. [Troubleshooting](#-troubleshooting)
16. [Author and Context](#-author-and-context)

---

## ✨ Features at a Glance

- **Community management** — register, edit, delete, list and search (by name or by ID).
- **Vehicle management** — register, edit, delete, list and search (by ID).
- **Five algorithm-design techniques** applied to the registered data:
  1. **Divide and Conquer** — community with the greatest need
  2. **Greedy** — distribution of supplies by priority
  3. **Dynamic Programming** — optimal selection of communities (0/1 knapsack)
  4. **Backtracking** — optimal delivery route (TSP type) with pruning
  5. **Branch and Bound** — assignment of vehicles to communities
- **Six sorted reports** using six different sorting algorithms (Quick, Merge, Insertion, Bubble, Selection and Shell sort).
- **Descriptive statistics** (mean, median, standard deviation, quartiles, coefficient of variation, Pearson correlation) with **automatic written conclusions**.
- **Excel export** (`.xlsx`) with five sheets, live formulas, native charts and conclusions.
- **Persistence with H2 + JDBC** — in-memory by default, switchable to a file database that keeps data between runs.
- **Input validation** with clear messages and custom exceptions.
- **Splash screen** with loading progress and a styled, tabbed Swing interface.

---

## 📋 Project Requirements

### Functional requirements

| ID | Requirement |
|----|-------------|
| FR-01 | Register, edit, delete, list and search **communities** (name, inhabitants, priority 1–10, required resources in tons). |
| FR-02 | Register, edit, delete, list and search **vehicles** (identifier, capacity in kg). |
| FR-03 | Persist communities and vehicles in a relational database through **JDBC** (H2). |
| FR-04 | Find the community with the greatest benefit (`inhabitants × priority`) using **Divide and Conquer**. |
| FR-05 | Distribute available supplies among communities by priority using a **Greedy** algorithm, reporting leftovers. |
| FR-06 | Select the communities that maximize total benefit for a limited shipment capacity using **Dynamic Programming** (0/1 knapsack). |
| FR-07 | Compute the shortest delivery route that leaves the distribution center, visits every community and returns, using **Backtracking** with pruning. |
| FR-08 | Assign vehicles to communities, maximizing coverage and minimizing wasted capacity, using **Branch and Bound**. |
| FR-09 | Generate community reports sorted by benefit, priority, required resources and name, and vehicle reports sorted by capacity and identifier. |
| FR-10 | Compute descriptive statistics and generate automatic conclusions. |
| FR-11 | Export the data, statistics and conclusions to an **Excel** workbook. |
| FR-12 | Validate every user input and report errors with understandable messages. |

### Non-functional requirements

| ID | Requirement |
|----|-------------|
| NFR-01 | **OOP and MVC**: separate domain, persistence, GUI and reporting layers. |
| NFR-02 | **Maintainability**: one DAO per entity; business rules in manager classes; utilities for validation. |
| NFR-03 | **Portability**: embedded database, no server and no internet needed at run time. |
| NFR-04 | **Usability**: splash screen, tabbed interface, consistent styling and clear messages. |
| NFR-05 | **Safe database access**: all queries use `PreparedStatement`; connection closed on exit. |
| NFR-06 | **Own algorithms**: sorting, searching and design techniques are implemented from scratch (no library sort). |
| NFR-07 | **Language**: user interface, reports and messages are in **Spanish**. |

### Software requirements

| Requirement | Version | Notes |
|-------------|---------|-------|
| **JDK** | **17 or newer** | Configured in `pom.xml` (`maven.compiler.source/target = 17`). |
| **Apache Maven** | 3.6+ | Build and dependency management (bundled with most IDEs). |
| **H2 Database** | 2.2.224 | Embedded SQL database, downloaded by Maven. |
| **Apache POI (`poi-ooxml`)** | 5.2.5 | Excel `.xlsx` generation, downloaded by Maven. |
| **Log4j Core** | 2.20.0 | Logging backend used by POI (avoids a `StatusLogger` warning). |
| **Swing** | Bundled with the JDK | Graphical interface. |
| **OS** | Windows, macOS or Linux | A graphical desktop is required. |

Maven dependencies (`pom.xml`):

```xml
com.h2database:h2:2.2.224
org.apache.poi:poi-ooxml:5.2.5
org.apache.logging.log4j:log4j-core:2.20.0
```

### Hardware / environment

- Any computer able to run Java 17 with a graphical desktop session.
- Internet is needed only the first time, to let Maven download the dependencies.

---

## 🔧 Installation

```bash
# 1. Get the project
git clone <repository-url>
cd AyudaHumanitaria

# 2. Compile and download dependencies
mvn clean compile

# 3. (Optional) build the JAR
mvn clean package
```

You can also open the folder as a **Maven project** in IntelliJ IDEA, NetBeans or Eclipse.

---

## ▶️ Running the Application

```bash
mvn exec:java
```

Or run the class `uce.edu.ec.ayudahumanitaria.ejecutable.Main` from your IDE.

On startup a **splash screen** shows the progress while the app (1) initializes the database, (2) loads the sample data if the tables are empty and (3) starts the interface. If something fails, a dialog explains the error and the program exits.

---

## 🧭 How to Use It (Step by Step)

The window has four tabs.

### 1 · Comunidades

1. Fill in **Nombre**, **Habitantes**, **Prioridad (1–10)** and **Recursos req. (ton)** and press **Registrar**.
2. Select a row in the table to edit it, then press **Modificar seleccionada**, or remove it with **Eliminar seleccionada**. **Nuevo** clears the form.
3. Search by **name** (partial text, case-insensitive) or by **ID**; **Listar todas** restores the full list. The list is ordered by priority (highest first).

### 2 · Vehículos

1. Enter the **ID** (text such as `V1`) and the **Capacidad (kg)** and press **Registrar**.
2. Edit, delete and search in the same way. The table shows whether each vehicle is assigned and to which community.

### 3 · Distribución

Each card runs one algorithm; the output appears in the **Resultados** panel.

| Card | What to enter | Result |
|------|---------------|--------|
| Community with the greatest need | — | Name and benefit of the winner. |
| Supply distribution (Greedy) | Available supplies (tons) | Amount given to each community, in priority order, and leftover. |
| Optimal selection (Knapsack) | Shipment capacity (whole tons) | Selected communities, total weight used and total benefit. |
| Optimal route (Backtracking) | — (2 to 8 communities) | Route `Centro → … → Centro`, total distance, nodes explored and branches pruned. |
| Vehicle assignment (Branch and Bound) | — | Vehicle → community pairs, wasted capacity, nodes explored and branches pruned. The assignment is **saved in the vehicles table**. |

### 4 · Reportes

- **Communities:** by benefit, by priority, by required resources, by name.
- **Vehicles:** by capacity, by identifier.
- **Ver estadísticas y conclusiones** shows the statistics and the automatic conclusions.
- **Generar Excel** saves the workbook (it asks before replacing an existing file and offers to open it afterwards).

---

## 🧮 The Problem Model

**Community** — `id` (auto), `nombre`, `habitantes`, `prioridad` (1–10), `recursosRequeridos` (tons).

```text
benefit = inhabitants × priority
```

**Vehicle** — `id` (text), `capacidad` (kg), `asignado` (boolean), `comunidadAsignada`.

**Sample data** loaded when the tables are empty:

| Community | Inhabitants | Priority | Resources (t) | Benefit |
|-----------|------------:|---------:|--------------:|--------:|
| Comunidad A | 5,000 | 9 | 4 | 45,000 |
| Comunidad B | 8,000 | 6 | 6 | 48,000 |
| Comunidad C | 6,500 | 6 | 5 | 39,000 |
| Comunidad D | 9,000 | 5 | 7 | 45,000 |

| Vehicle | Capacity |
|---------|---------:|
| V1 | 1,000 kg |
| V2 | 1,500 kg |

---

## 🧠 The Five Algorithm-Design Techniques

All of them are in `dominio/Algoritmos.java`.

### ① Divide and Conquer — *community with the greatest need*
Recursively splits the array in halves, finds the best community in each half and returns the one with the higher benefit. Complexity **O(n)** with **O(log n)** recursion depth.

### ② Greedy — *distribution of supplies*
Sorts the communities by priority (highest first) and gives each one `min(required resources, remaining supplies)` until the supplies run out; the rest receive 0. The leftover is reported. The greedy choice (always serve the most urgent community first) is simple and fast, but it is not guaranteed to maximize total benefit.

### ③ Dynamic Programming — *optimal selection (0/1 knapsack)*
Weight of each community = its required resources (rounded to whole tons); value = its benefit. A table `dp[i][w]` stores the best benefit using the first `i` communities with capacity `w`; the selected communities are recovered by walking the table backwards. Complexity **O(n · capacity)**.

### ④ Backtracking — *optimal delivery route (TSP type)*
Explores every order of visits starting and ending at **Centro**, building the route one community at a time. A branch is **pruned** as soon as its accumulated distance is not better than the best complete route found. The result reports the route, the total distance, **nodes explored** and **branches pruned**. Limited to **8 communities** (factorial growth).
*Distances between points are demonstration data generated with `10 + |i − j| × 8`.*

### ⑤ Branch and Bound — *assignment of vehicles*
Communities are sorted by required resources (largest first) and each one either receives a free vehicle whose capacity is enough or is left unserved. A branch is discarded when the **upper bound** of coverage (current coverage + min(free vehicles, remaining communities)) cannot beat the best solution, or ties it without wasting less capacity. Objective: **maximum coverage first, minimum wasted capacity second**. The best assignment is saved in the vehicles table.

---

## 🔎 Sorting and Searching Algorithms

| Algorithm | Complexity (typical) | Where it is used |
|-----------|----------------------|------------------|
| **Quick sort** | O(n log n) | Default community / vehicle listings, benefit report, searches, greedy ordering. |
| **Merge sort** | O(n log n) | Communities by priority report. |
| **Insertion sort** | O(n²) | Communities by required resources report. |
| **Bubble sort** | O(n²) | Communities by name report. |
| **Selection sort** | O(n²) | Vehicles by capacity report. |
| **Shell sort** | ~O(n^1.3) | Vehicles by identifier report. |
| **Sequential search** | O(n) | Generic search by condition. |
| **Binary search (iterative)** | O(log n) | Search on sorted arrays. |
| **Binary search (recursive)** | O(log n) | Find a community / vehicle by ID (after sorting by ID). |

All of them are generic (`<T>` with a `Comparator`), so the same code sorts communities and vehicles.

---

## 📊 Reports, Statistics and Excel Export

### On-screen reports

| Report | Sorting algorithm |
|--------|-------------------|
| Communities by benefit (descending) | Quick sort |
| Communities by priority (descending) | Merge sort |
| Communities by required resources (ascending) | Insertion sort |
| Communities by name | Bubble sort |
| Vehicles by capacity (descending) | Selection sort |
| Vehicles by identifier | Shell sort |

### Statistics (`EstadisticaDescriptiva`, `AnalisisAyuda`)

- For inhabitants, priority, required resources, benefit and vehicle capacity: **n, sum, mean, median, sample standard deviation (n−1), minimum, Q1, Q3, maximum, range and coefficient of variation**. Quartiles use linear interpolation (equivalent to Excel's `QUARTILE.INC`).
- **Pearson correlation** between inhabitants and required resources.
- Totals: total benefit, total required resources vs. total fleet capacity (tons), **coverage %**, deficit, assigned vehicles and **high-priority communities** (priority ≥ 8).
- **Automatic conclusions** in prose: scope, population variability (low / moderate / high), community with the greatest need and its share of the benefit, priorities, population–resources relationship (strong / moderate / weak) and logistic capacity.

### Excel workbook (`AyudaHumanitaria_<timestamp>.xlsx`)

| Sheet | Content |
|-------|---------|
| **Resumen** | Key figures and two bar charts: benefit per community and required resources (t) per community. |
| **Comunidades** | Community data and benefit. |
| **Vehiculos** | Vehicle data and assignment status. |
| **Estadisticas** | Descriptive statistics written as **live Excel formulas** (they recalculate if you change the data). |
| **Conclusiones** | The automatic conclusions, numbered. |

---

## 🗄 Database

The project uses **H2** through JDBC (`ConexionBD`). Two modes are available by editing the `URL` constant:

| Mode | URL | Behavior |
|------|-----|----------|
| **In memory** (default) | `jdbc:h2:mem:ayudahumanitaria;DB_CLOSE_DELAY=-1` | Data is lost on exit; sample data is reloaded on every run. |
| **File** | `jdbc:h2:file:./data/ayudahumanitaria` | Data persists in `./data/ayudahumanitaria.mv.db`. |

Tables created automatically (`CREATE TABLE IF NOT EXISTS`):

```sql
CREATE TABLE COMUNIDAD (
    ID                  INT AUTO_INCREMENT PRIMARY KEY,
    NOMBRE              VARCHAR(100) NOT NULL,
    HABITANTES          INT          NOT NULL,
    PRIORIDAD           INT          NOT NULL,
    RECURSOS_REQUERIDOS DOUBLE       NOT NULL
);

CREATE TABLE VEHICULO (
    ID                  VARCHAR(20) PRIMARY KEY,
    CAPACIDAD           DOUBLE      NOT NULL,
    ASIGNADO            BOOLEAN     DEFAULT FALSE,
    COMUNIDAD_ASIGNADA  VARCHAR(100)
);
```

Credentials: user `sa`, empty password (embedded database).

---

## ✅ Validation Rules

| Field | Rule | Message |
|-------|------|---------|
| Name / vehicle ID | Not empty | “El campo … no puede estar vacío.” |
| Inhabitants, required resources, capacity | Greater than 0 | “El campo … debe ser un valor positivo.” |
| Priority | Integer from 1 to 10 | “La prioridad debe estar entre 1 y 10.” |
| Numeric text fields | Must be numeric | “Ingrese una … numérica válida.” |

Validation errors use `ValidationException` and database errors use `PersistenciaException`; both carry user-friendly messages up to the interface.

---

## 🧱 Project Structure

```text
AyudaHumanitaria/
├── pom.xml                                    # Maven configuration (Java 17)
└── src/main/java/uce/edu/ec/ayudahumanitaria/
    ├── ejecutable/
    │   └── Main.java                          # Entry point: splash + initialization
    ├── dominio/                               # Model / business logic
    │   ├── Comunidad.java                     # Community entity (benefit calculation)
    │   ├── Vehiculo.java                      # Vehicle entity
    │   ├── GestorComunidades.java             # Community rules + DAO access
    │   ├── GestorVehiculos.java               # Vehicle rules + DAO access
    │   ├── Algoritmos.java                    # Sorting, searching and design techniques
    │   ├── EstadisticaDescriptiva.java        # Descriptive statistics
    │   ├── AnalisisAyuda.java                 # Aggregate analysis and conclusions
    │   └── DatosEjemplo.java                  # Sample data
    ├── persistencia/                          # JDBC / H2
    │   ├── ConexionBD.java                    # Connection and schema
    │   ├── ComunidadDAO.java
    │   ├── VehiculoDAO.java
    │   └── PersistenciaException.java
    ├── gui/                                   # Swing (view / controller)
    │   ├── VentanaPrincipal.java              # Main window with 4 tabs
    │   ├── PanelComunidades.java
    │   ├── PanelVehiculos.java
    │   ├── PanelAlgoritmos.java               # "Distribución" tab
    │   ├── PanelReportes.java
    │   ├── SplashScreen.java
    │   └── EstiloUI.java                      # Colors, buttons and cards
    ├── reportes/
    │   └── ExportadorExcel.java               # Excel export with Apache POI
    └── util/
        ├── Validador.java
        └── ValidationException.java
```

---

## 🏗 Architecture and Design Notes

```text
 GUI (Swing panels) ──► Managers (dominio) ──► DAO (persistencia) ──► H2 (JDBC)
         │                     │
         │                     └──► Algoritmos / Estadística
         └──► ExportadorExcel (Apache POI)
```

- **MVC with a persistence layer:** `dominio` is the model and knows nothing about Swing; `gui` is the view/controller; `persistencia` is the only layer that touches SQL.
- **Manager classes** (`GestorComunidades`, `GestorVehiculos`) apply validation before delegating to the DAO and return sorted arrays.
- **Generic algorithms:** `Algoritmos` is a utility class with generic methods and result holder classes (`ResultadoDistribucion`, `ResultadoMochila`, `ResultadoRuta`, `ResultadoAsignacion`) that also report search effort (nodes explored / pruned).
- **Responsive startup:** database initialization and sample-data loading run in a `SwingWorker` while the splash screen animates, keeping the UI thread free.
- **Clean shutdown:** a JVM shutdown hook closes the database connection.
- **Custom exceptions** translate low-level errors into messages the user can read.

---

## ⚠️ Assumptions and Limitations

This is an **educational project**, not an operational logistics system.

- **Route distances are demonstration data** (`10 + |i − j| × 8`), not real distances, and the route algorithm is limited to **8 communities**.
- **Knapsack capacity is a whole number of tons**, and each community's weight is its required resources rounded to a whole ton.
- **Units in the vehicle assignment:** communities store resources in **tons** and vehicles store capacity in **kg**; the Branch and Bound step compares the two numbers directly (no ton↔kg conversion) and the on-screen summary prints both in kg. Keep this in mind when entering your own data.
- The Greedy distribution favors priority and does not guarantee the maximum total benefit.
- With the default **in-memory** database, data is lost when the program closes.
- The interface, reports and messages are in **Spanish**; there is no command-line or web version.

---

## 🛠 Troubleshooting

| Problem | Solution |
|---------|----------|
| `release version 17 not supported` / compilation error | Install JDK 17+ and make sure `JAVA_HOME` points to it (`java -version`, `mvn -v`). |
| `mvn` is not recognized | Install Apache Maven and add it to your `PATH`, or run the project from your IDE. |
| Dependencies do not download | Check your internet connection (first build only) or proxy settings for Maven. |
| “Error crítico” dialog on startup | The database could not be initialized; check the message and that no other process locks the `./data` folder when using file mode. |
| Data disappears after closing | The in-memory mode is active; switch the `URL` in `ConexionBD.java` to the file mode. |
| Excel cannot be saved | The target `.xlsx` is open in Excel or the folder is read-only; close the file or choose another location. |
| “Por eficiencia, esta demo admite hasta 8 comunidades” | The route algorithm is limited to 8 communities; delete some communities or use another algorithm. |

---

## 👤 Author and Context

- **Author:** Christopher Josué Villón Peralta
- **Context:** integrative group project — Universidad Central del Ecuador, Faculty of Engineering and Applied Sciences.
- **License:** *not specified yet* — add a `LICENSE` file (e.g., MIT) if you want others to reuse the code.

---

*This software is for educational purposes.*
