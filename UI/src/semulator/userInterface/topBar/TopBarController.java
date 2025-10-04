package semulator.userInterface.topBar;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import semulator.logic.functions.Function;
import semulator.logic.program.Sprogram;
import semulator.logic.xml.xmlreader.XMLParser;
import semulator.userInterface.mainBar.AppController;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TopBarController {

    @FXML private TextField pathField;
    @FXML private ComboBox<FunctionItem> cmbFunctions;

    private final javafx.collections.ObservableList<FunctionItem> fnItems =
            javafx.collections.FXCollections.observableArrayList();
    private final Map<String, FunctionItem> fnIndex = new LinkedHashMap<>();

    private AppController app;
    public void setAppController(AppController app) { this.app = app; }

    // אייטם לתצוגה – מציג name ו-"from: name"
    public static final class FunctionItem {
        public final String functionName;       // name
        public final String fromProgramName;    // name של התכנית
        public final Function ref;
        public FunctionItem(String functionName, String fromProgramName, Function ref) {
            this.functionName = functionName;
            this.fromProgramName = fromProgramName;
            this.ref = ref;
        }
        @Override public String toString() { return functionName + "   —   from: " + fromProgramName; }
    }

    /** נקראת מ-AppController.onProgramLoaded(program, sourcePath) */
    public void refreshFunctionList(Sprogram program, String sourcePath){
        if (program == null) return;
        if (program instanceof semulator.logic.program.SprogramImpl impl) {
            final String programName = impl.getName();
            for (Function f : impl.getFunctions()) {
                String key = f.getName();
                if (!fnIndex.containsKey(key)) {
                    FunctionItem item = new FunctionItem(f.getName(), programName, f);
                    fnIndex.put(key, item);
                    fnItems.add(item);
                }
            }
        }
        if (cmbFunctions != null && cmbFunctions.getItems() != fnItems) {
            cmbFunctions.setItems(fnItems);
        }
        if (!fnItems.isEmpty() && (cmbFunctions != null) &&
                cmbFunctions.getSelectionModel().isEmpty()) {
            cmbFunctions.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onLoadSelectedFunction(ActionEvent e){
        FunctionItem sel = (cmbFunctions != null)
                ? cmbFunctions.getSelectionModel().getSelectedItem() : null;
        if (sel == null) return;
        Sprogram exec = sel.ref.getInstructionExecuteProgram();
        if (exec == null) {
            showError("Load Function", "Selected function has no execute program.");
            return;
        }
        if (app != null) {
            app.onProgramLoaded(exec, (pathField != null ? pathField.getText() : null));
        } else {
            showError("App wiring error", "AppController is null – ensure setAppController() is called.");
        }
    }

    @FXML
    private void onBrowseFile(ActionEvent e) {
        Window owner = ((Node) e.getSource()).getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose XML file");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        if (pathField.getText() != null && !pathField.getText().isBlank()) {
            File hint = new File(pathField.getText()).getParentFile();
            if (hint != null && hint.isDirectory()) fc.setInitialDirectory(hint);
        }

        File f = fc.showOpenDialog(owner);
        if (f != null) {
            pathField.setText(f.getAbsolutePath());
            loadXmlIntoAppAsync(f.getAbsolutePath(), owner);
        }
    }

    private void loadXmlIntoAppAsync(String filePath, Window owner) {
        Stage progressStage = createProgressStage(owner);
        ProgressBar bar = (ProgressBar) ((VBox) progressStage.getScene().getRoot()).getChildren().get(0);
        Label label     = (Label)     ((VBox) progressStage.getScene().getRoot()).getChildren().get(1);

        Task<Sprogram> task = new Task<>() {
            @Override
            protected Sprogram call() throws Exception {
                updateProgress(0, 1);
                updateMessage("Validating path…");
                XMLParser.validateXmlFilePath(filePath);

                updateProgress(0.25, 1);
                updateMessage("Parsing XML…");
                XMLParser parser = new XMLParser();
                Sprogram program = parser.loadProgramFromXML(filePath);

                // --- ולידציה נוספת: פונקציות בשימוש קיימות בתכנית ---
                updateProgress(0.55, 1);
                updateMessage("Validating used functions…");
                validateUsedFunctions(program);

                updateProgress(0.75, 1);
                updateMessage("Validating program…");
                if (program == null) throw new IllegalStateException("Loaded program is null.");
                if (!program.validate())
                    throw new IllegalStateException("Program validation failed: a jump targets an undefined label.");

                updateProgress(1, 1);
                updateMessage("Done");
                return program;
            }
        };

        bar.progressProperty().bind(task.progressProperty());
        label.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(ev -> {
            progressStage.close();
            Sprogram program = task.getValue();
            if (app != null) {
                Platform.runLater(() -> app.onProgramLoaded(program, filePath));
                Platform.runLater(() -> refreshFunctionList(program, filePath));
            } else {
                showError("App wiring error", "AppController is null – please ensure setAppController() is called.");
            }
        });

        task.setOnFailed(ev -> {
            progressStage.close();
            Throwable ex = task.getException();
            String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : String.valueOf(ex);
            showError("Failed to load program", msg);
        });

        progressStage.show();
        Thread t = new Thread(task, "xml-load-task");
        t.setDaemon(true);
        t.start();
    }

    private Stage createProgressStage(Window owner) {
        ProgressBar pb = new ProgressBar(0);
        pb.setPrefWidth(280);
        Label msg = new Label("Starting…");

        VBox box = new VBox(10, pb, msg);
        box.setStyle("-fx-padding: 14;");

        Stage s = new Stage();
        s.initOwner(owner);
        s.initModality(Modality.WINDOW_MODAL);
        s.setTitle("Loading");
        s.setResizable(false);
        s.setScene(new Scene(box));
        return s;
    }

    // אופציונלי לשימוש פנימי/בדיקות
    public void loadProgramFromPath(String path, Runnable onSuccess) {
        Window owner = (pathField != null && pathField.getScene()!=null) ? pathField.getScene().getWindow() : null;
        Stage progressStage = createProgressStage(owner);
        Task<Sprogram> task = new Task<>() {
            @Override protected Sprogram call() throws Exception {
                updateMessage("Validating path…");
                XMLParser.validateXmlFilePath(path);
                updateMessage("Parsing XML…");
                XMLParser parser = new XMLParser();
                Sprogram program = parser.loadProgramFromXML(path);

                // --- ולידציה נוספת גם בזרימה הזו ---
                updateMessage("Validating used functions…");
                validateUsedFunctions(program);

                return program;
            }
        };
        task.setOnSucceeded(ev -> {
            progressStage.close();
            Sprogram program = task.getValue();
            if (app != null) app.onProgramLoaded(program, path);
            refreshFunctionList(program, path);
            if (onSuccess != null) onSuccess.run();
        });
        task.setOnFailed(ev -> {
            progressStage.close();
            Throwable ex = task.getException();
            showError("Failed to load XML", (ex!=null && ex.getMessage()!=null)? ex.getMessage(): String.valueOf(ex));
        });
        progressStage.show();
        new Thread(task, "xml-load-task").start();
    }

    // בדיקת "פונקציות בשימוש" מול "פונקציות מוגדרות" לפי שם פונקציה
    private static void validateUsedFunctions(Sprogram program) {
        if (program == null) throw new IllegalStateException("Loaded program is null.");
        List<Function> declared = program.getFunctions();
        List<Function> used     = program.getUseFunctions(); // קיימת לפי הדרישה

        if (used == null || used.isEmpty()) return; // אין תלות, הכל תקין

        Set<String> declaredNames = (declared == null ? Set.<String>of()
                : declared.stream().map(Function::getName).collect(Collectors.toSet()));

        List<String> missing = used.stream()
                .map(Function::getName)
                .filter(n -> n != null && !n.isBlank())
                .filter(n -> !declaredNames.contains(n))
                .distinct()
                .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            String msg = "Program validation failed: referenced functions not found: " +
                    String.join(", ", missing) + ".";
            throw new IllegalStateException(msg);
        }
    }

    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initOwner(pathField.getScene().getWindow());
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(content);

        var dp = a.getDialogPane();
        String dialogCss = getClass()
                .getResource("/semulator/userInterface/mainBar/dialogs-dark.css")
                .toExternalForm();
        dp.getStylesheets().add(dialogCss);
        dp.getStyleClass().add("app-dialog");
        dp.getStylesheets().addAll(pathField.getScene().getStylesheets());

        a.showAndWait();
    }
}
