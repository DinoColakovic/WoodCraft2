# WoodCraft2 AI Coding Guidelines

## Architecture Overview
WoodCraft2 is a JavaFX-based woodworking design application with an MVC-inspired structure:
- **UI Layer** (`ui/`): JavaFX components like `MainView.java` (main canvas interface), `CanvasPane.java` (drawing surface), dialogs. Uses programmatic UI building with some FXML.
- **Service Layer** (`service/`): Business logic including `GeometryService.java` (shape detection/cutting), `EstimationService.java` (material cost calculations), `AuthService.java` (user authentication).
- **Data Layer** (`dao/`, `db/`): Raw JDBC DAOs for SQLite database access. No ORM; uses prepared statements and `IllegalStateException` for SQL errors.
- **Model Layer** (`model/`): Simple POJOs for entities like `Document`, `Material`, `ShapePolygon`.
- **Session Management** (`session/`): `SessionManager.java` handles user sessions and role-based access (ADMIN/USER).

Data flows from user interactions (canvas clicks) → geometry computation → shape storage → cost estimation → UI updates.

## Key Workflows
- **Build & Run**: Use `mvn clean javafx:run` to launch the app. Main class is `AppLauncher.java`.
- **Database**: SQLite DB auto-created in `~/.woodcraft/woodcraft.db`. Schema initialized via `DatabaseInitializer.java`.
- **Testing**: JUnit 5 tests in `src/test/`. Run with `mvn test`.
- **Debugging**: Attach debugger to JavaFX app; breakpoints in `MainView.java` event handlers for UI interactions.

## Project Conventions
- **Package Naming**: Deep nesting (`unze.ptf.woodcraft.woodcraft`) - maintain exact structure.
- **Error Handling**: DAOs throw `IllegalStateException` for database failures; services may return null or empty collections.
- **Units**: Dual support (CM/IN) via `UnitSystem` enum; conversions in `UnitConverter.java`.
- **UI Labels**: Croatian text hardcoded (e.g., "Odabrani oblik"); preserve for localization.
- **Geometry**: Complex polygon operations in `GeometryService.java`; prefer reusing existing methods over reimplementing math.
- **Estimation**: Waste percentage applied globally; materials have types (`MaterialType`) affecting pricing.
- **History/Undo**: Snapshot-based undo in `MainView.java` using `ProjectSnapshot` records.

## Integration Points
- **PDF Export**: Uses Apache PDFBox in `PdfExportService.java` for canvas snapshots.
- **Authentication**: jBCrypt for password hashing; role-based UI (admin menu in `MainView.java`).
- **Canvas Interactions**: Event-driven updates; snapping to guides/dimensions in `MainView.java` `applyGuideSnapping()`.

Reference: `MainView.java` for UI patterns, `UserDao.java` for DAO structure, `GeometryService.java` for algorithms.</content>
<parameter name="filePath">c:\Users\adnic\OneDrive\Desktop\FitnessApp\WoodCraft2\.github\copilot-instructions.md