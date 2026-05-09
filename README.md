# **🎲 Monopoly Deal (Java Edition) - Project ENG-19**

## Course Project: Software Engineering / Advanced OOP

This project is a Java-based digital implementation of the popular card game Monopoly Deal. Beyond implementing the game rules, the core architecture is built upon Object-Oriented Programming (OOP) principles and industry-standard Design Patterns to ensure high cohesion, low coupling, and scalability.

## ✨ Design Patterns & Architecture

To eliminate "Code Smells" and ensure a robust engine, we have implemented the following design patterns:

### Singleton Pattern

Implementation: GameManager (The Central Controller).

Purpose: Ensures a single point of truth for the game state, managing turn flow, state transitions, and player interactions globally.

### Observer Pattern

Implementation: Decoupling GameManager from the UI layer.

Purpose: The backend notifies the UI via a notifyEvent() mechanism. This allows the JavaFX frontend to update dynamically without being tightly coupled to the game logic.

### State Machine & Command Pattern

Implementation: Asynchronous interruption handling for "Just Say No" cards.

Purpose: Malicious actions (e.g., Sly Deal, Rent) are encapsulated as Runnable commands and suspended. The game enters a "Waiting" state for the victim's response, preventing UI thread blocking and avoiding primitive infinite loops.

### Decorator Pattern

Implementation: Property Rent System (HouseDecorator, HotelDecorator).

Purpose: Dynamically adds rent bonuses to a completed PropertySet. This adheres to the Open-Closed Principle (OCP), allowing us to add new types of improvements without modifying existing property classes.

## 👥 Team Roles & Responsibilities

The project was developed using a modular collaborative approach:

Member 1 (Core Architect): Developed the GameManager engine, State Machine for card interruptions, and the JUnit testing suite.

Member 2 (UI Developer): Developed the JavaFX frontend, event subscription systems, and visual asset management.

Member 3 (Property Specialist): Implemented the Property system, Wild Card color-switching logic, and the Decorator-based rent calculator.

Member 4 (Bank & Debt Management): Managed the player Bank area, debt collection logic, and forced liquidation mechanics.

Member 5 (Action Logic Expert): Implemented specific business logic for Action cards (e.g., Sly Deal, Force Deal, Debt Collector).

## 🛠️ Technical Specifications

Language: Java 17+ (LTS)

UI Framework: JavaFX

Testing Framework: JUnit 5 (Jupiter)

Build System: IntelliJ IDEA Project Structure / Maven (Optional)

JavaFX Dependency: The project includes OpenJFX 17.0.2 jars under `lib`, referenced by `ENG-19.iml`, so teammates do not need to configure a separate JavaFX SDK manually. If IntelliJ still reports class file version 68.0, remove any existing JavaFX 24 library from Project Structure and reload this project.

## 🚀 Getting Started

### Prerequisites

Java Development Kit (JDK) 17 or higher.

JavaFX is already included under the project `lib` folder.

### Running the Game

Clone the repository to your local machine.

Open the project in your IDE (IntelliJ IDEA recommended).

Navigate to src/Main.java (or the designated entry point).

Run the main method. The game engine and UI will initialize automatically.

### 🧪 Testing Suite

We maintain a rigorous testing standard. All unit tests are located in the test directory, mirroring the src package structure.

### How to run tests:

Right-click the test folder in your IDE.

Select "Run 'All Tests'".

### Key Test Coverage:

DeckTest: Validates deck initialization and the automatic reshuffling logic when the draw pile is exhausted.

GameManagerTest: Ensures correct turn flow and the strict deduction of 3 action points per turn.

DecoratorTest: Verifies the mathematical accuracy of rent calculation when multiple decorators (House/Hotel) are applied.
