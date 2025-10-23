# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is a JavaFX desktop application (Cotizador VSS) built with Maven. It's a quotation/invoicing system with a tabbed interface for managing products, generating quotes, and viewing reports.

**Key Technologies:**
- JavaFX 17.0.2 for UI
- Maven for build management
- JUnit 5 for testing
- Java 17 (project configured for Java 17, but user may use Java 7 for legacy compatibility)

## Essential Commands

### Build & Compile
```bash
mvn clean compile
```

### Run Application
```bash
mvn javafx:run
```

### Run Tests
```bash
mvn test
```

### Run Single Test
```bash
mvn test -Dtest=MainTest
mvn test -Dtest=MainTest#testMainClassExists
```

### Clean Project
```bash
mvn clean
```

### Install Dependencies
```bash
mvn clean compile
```

## Architecture

### Application Structure
The application uses a **single-class monolithic design** with all UI logic in `Main.java`:

1. **Main.java** (`com.example.Main`)
   - Entry point extending `javafx.application.Application`
   - Contains inner `Producto` class for data model
   - Implements 4-tab interface: Dashboard, Productos, Cotizaciones, Reportes
   - Uses JavaFX TableView with ObservableList for reactive product management
   - All event handlers and UI construction in `start()` method

### UI Components
- **TabPane**: Main navigation container with 4 non-closable tabs
- **TableView<Producto>**: Product grid with columns (nombre, precio, cantidad, categoria)
- **ObservableList**: Data binding pattern for automatic UI updates
- **PropertyValueFactory**: Maps table columns to Producto properties

### Data Flow
Products are stored in an `ObservableList<Producto>` that automatically synchronizes with the TableView. Adding/modifying products through the UI controls updates both the underlying list and the table display.

## Code Patterns

### Adding New UI Elements
Follow the existing pattern in `Main.java`:
1. Create VBox/HBox layout with padding (typically `new Insets(20)`)
2. Add controls with prompt text for TextField inputs
3. Wire event handlers using lambda expressions
4. Use Alert dialogs for user feedback

### TableView Pattern
```java
TableColumn<Producto, Type> col = new TableColumn<>("Header");
col.setCellValueFactory(new PropertyValueFactory<>("propertyName"));
table.getColumns().add(col);
```

### Data Model
Inner classes inside `Main` are used for data structures. The `Producto` class follows standard JavaBean conventions with private fields and public getters/setters.

## Important Notes

- The pom.xml specifies Java 17, but UTF-8 encoding is set via `System.setProperty` in main()
- All UI text is in Spanish (labels, messages, categories)
- No FXML is used; all UI is built programmatically in Java
- The JavaFX Maven Plugin requires the `mainClass` property set to `com.example.Main`
- Tests are minimal and primarily verify class structure rather than UI behavior
