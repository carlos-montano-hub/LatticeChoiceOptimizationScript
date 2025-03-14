# Greedy Algorithm for Lattice Selection in Data Warehouses

## Overview

This Java script implements a greedy algorithm to determine the optimal sequence of lattices to materialize for a data warehouse. The goal is to enhance query performance while minimizing storage costs.

## Features

- **Efficient Lattice Selection**: Uses a greedy approach to select the most beneficial lattices.
- **Performance Optimization**: Improves query performance by materializing the most impactful views.
- **Resource Management**: Balances between data insight gains and resource constraints.

## Prerequisites

- Java Development Kit (JDK) 17 or higher
- Any Java IDE (e.g., IntelliJ IDEA, Eclipse) or a text editor

## Installation

1. Clone the repository:

```bash
   git clone https://github.com/carlos-montano-hub/LatticeChoiceOptimizationScript.git
```

2. Navigate to the project directory:

```
bash   cd greedy-lattice-selection
```

## Usage

1. Update data.json:
   inside the root directory there is a file called "data.json" this file contains an example of a lattice tree. This example can be found explained in [Implementing Data Cubes Efficiently](https://web.eecs.umich.edu/~jag/eecs584/papers/implementing_data_cube.pdf) section 4, example 4.1

```bash
mvn compile
```

2.  Run the main class:

```bash
mvn exec:java -Dexec.mainClass="org.example.HypercubeLatticeOptimization"
```

It should print the optimal lattices as well as each step of the calculation.

## Contributing

Contributions are welcome! Please fork the repository and create a pull request with your changes.

## License

This project is licensed under the GPL-3.0 License. See the [LICENSE](LICENSE) file for details.

## Contact

For any questions or suggestions, please contact montanoc70@gmail.com.
