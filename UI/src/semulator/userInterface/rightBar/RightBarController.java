package semulator.userInterface.rightBar;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import semulator.display.RunRecord;
import semulator.logic.execution.ProgramExecutorImpl;
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;
import semulator.userInterface.mainBar.AppController;

import java.util.*;

public class RightBarController {

    // --- חיבור לאפליקציה ---
    private AppController app;
    public void setAppController(AppController app) { this.app = app; }

    // --- מודל/מצב ---
    private Sprogram program;
    private ProgramExecutorImpl executor;
    private final Map<String, TextField> inputFields = new LinkedHashMap<>();
    private List<String> inputOrder = new ArrayList<>();
    private boolean addToHistory = true; // לריסת Re-Run

    // מצב/תצוגה
    @FXML private ToggleGroup modeGroup;
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Button btnStepOver;
    @FXML private Button btnShow;     // Show לפי הנבחר בטבלת ההיסטוריה הימנית
    @FXML private Button btnRerun;    // Re-Run לפי הנבחר בטבלת ההיסטוריה הימנית
    @FXML private Label  lblCycles;
    @FXML private VBox   inputsBox;

    // --- טבלת היסטוריה ימין (כפי שביקשת) ---
    @FXML private TableView<RunRecord>             tblRunHistoryRight;     // fx:id
    @FXML private TableColumn<RunRecord, Number>   colRunIndexRight;       // fx:id
    @FXML private TableColumn<RunRecord, String>   colRunProgramRight;     // fx:id
    private final javafx.collections.ObservableList<RunRecord> runRows = javafx.collections.FXCollections.observableArrayList();

    // תצוגת STATE (אם יש לך ב-FXML)
    @FXML private VBox  varsBox;
    @FXML private Label lblVarsTitle;

    @FXML
    private void initialize() {
        wireFlash(btnStart);
        wireFlash(btnStop);
        wireFlash(btnStepOver);
        wireFlash(btnShow);
        wireFlash(btnRerun);

        if (btnStart != null) btnStart.setOnAction(e -> onStartClicked());
        if (btnShow  != null) btnShow.setOnAction(e -> onShowClicked());
        if (btnRerun != null) btnRerun.setOnAction(e -> onRerunClicked());

        // טבלת היסטוריה ימנית – 2 עמודות: מספר ריצה + שם תכנית
        if (tblRunHistoryRight != null) {
            tblRunHistoryRight.setItems(runRows);
        }
        if (colRunIndexRight != null)  colRunIndexRight.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().getRunNumber()));
        if (colRunProgramRight != null) colRunProgramRight.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getProgramName()));

        inputsBox.sceneProperty().addListener((obs, o, n) -> { if (n != null) applyMode(); });
        if (modeGroup != null) modeGroup.selectedToggleProperty().addListener((obs, o, n) -> applyMode());
        Platform.runLater(this::applyMode);
        updateCyclesTitle();
    }

    public void bindProgram(Sprogram program) {
        this.program = program;
        rebuildInputsFromProgram();
        updateCyclesTitle();
    }

    private String debugCssUrl;
    private void applyMode() {
        var scene = (btnStart != null) ? btnStart.getScene() : null;
        if (scene == null) return;

        // תמה (צהוב) במצב DEBUG
        if (debugCssUrl == null) {
            debugCssUrl = getClass()
                    .getResource("/semulator/userInterface/mainBar/theme-debug.css")
                    .toExternalForm();
        }
        boolean debug = "DEBUG".equalsIgnoreCase(currentMode());

        if (!scene.getStylesheets().contains(debugCssUrl)) {
            scene.getStylesheets().add(debugCssUrl);
        }
        var rootClasses = scene.getRoot().getStyleClass();
        if (debug) {
            if (!rootClasses.contains("debug-theme")) rootClasses.add("debug-theme");
        } else {
            rootClasses.remove("debug-theme");
        }

        if (debug) {
            // כפתורים מותרים במצב DEBUG
            java.util.Set<String> allow = new java.util.HashSet<>();
            allow.add("btnStart");
            allow.add("btnStop");
            allow.add("btnStepOver");
            allow.add("LoadButtonTB");

            for (javafx.scene.Node n : scene.getRoot().lookupAll(".button")) {

                // אם זה ToggleButton ששייך ל-modeGroup — משאירים פעיל
                if (n instanceof ToggleButton tb) {
                    boolean inModeGroup = (modeGroup != null && modeGroup.getToggles().contains(tb));
                    if (inModeGroup) {
                        tb.setDisable(false);
                        continue;
                    }
                }

                // כל היתר: מאפסים לפי רשימת ההיתר
                if (n instanceof Button b) {
                    String id = b.getId();
                    boolean allowed = (id != null && allow.contains(id));
                    b.setDisable(!allowed);
                }
            }

            // הבטחת מצב הכפתורים המותרים בימין
            if (btnStart    != null) btnStart.setDisable(false);
            if (btnStop     != null) btnStop.setDisable(false);
            if (btnStepOver != null) btnStepOver.setDisable(false);

        } else {
            // EXECUTE: להחזיר הכל לזמין, ואז לכבות Stop/StepOver
            for (javafx.scene.Node n : scene.getRoot().lookupAll(".button")) {
                if (n instanceof Button b) {
                    b.setDisable(false);
                } else if (n instanceof ToggleButton tb) {
                    tb.setDisable(false);
                }
            }
            if (btnStop     != null) btnStop.setDisable(true);
            if (btnStepOver != null) btnStepOver.setDisable(true);
            if (btnStart    != null) btnStart.setDisable(false);
        }
    }


    private String currentMode() {
        if (modeGroup == null || modeGroup.getSelectedToggle() == null) return "EXECUTE";
        Toggle t = modeGroup.getSelectedToggle();
        if (t instanceof ToggleButton tb) {
            String at = tb.getAccessibleText();
            if (at != null && !at.isBlank()) return at.trim().toUpperCase();
            Object ud = tb.getUserData();
            if (ud instanceof String s && !s.isBlank()) return s.trim().toUpperCase();
            String txt = tb.getText();
            if (txt != null && !txt.isBlank()) return txt.trim().toUpperCase();
        }
        return "EXECUTE";
    }

    private void rebuildInputsFromProgram() {
        inputFields.clear();
        inputOrder.clear();
        inputsBox.getChildren().clear();

        if (program == null) return;

        executor   = new ProgramExecutorImpl(program);
        inputOrder = executor.getInputLabelsNames();
        if (inputOrder == null) inputOrder = new ArrayList<>();

        for (int i = 0; i < inputOrder.size(); i++) {
            String varName = inputOrder.get(i);

            Label nameLbl = new Label((i + 1) + ". " + varName + ":");
            nameLbl.getStyleClass().add("rightbar-input-label");
            nameLbl.setMinWidth(70);
            nameLbl.setTooltip(new Tooltip("Input for variable " + varName));

            TextField tf = new TextField();
            tf.setPromptText("0");
            tf.getStyleClass().add("rightbar-input-field");
            HBox.setHgrow(tf, Priority.ALWAYS);

            HBox row = new HBox(8, nameLbl, tf);
            row.getStyleClass().add("rightbar-input-row");
            inputsBox.getChildren().add(row);

            inputFields.put(varName, tf);
        }

        // נקה STATE מוצג
        if (varsBox != null) varsBox.getChildren().clear();
        if (lblVarsTitle != null) lblVarsTitle.setVisible(false);
    }

    // --- START: ריצה ושמירה להיסטוריה ---
    private void onStartClicked() {
        if (!"EXECUTE".equalsIgnoreCase(currentMode())) return;
        if (executor == null || program == null) {
            showError("No program", "Please load a program first.");
            return;
        }

        List<Long> values = new ArrayList<>(inputOrder.size());
        String[] inputsSnapshot = new String[inputOrder.size()];
        for (int i = 0; i < inputOrder.size(); i++) {
            String name = inputOrder.get(i);
            TextField tf = inputFields.get(name);
            String raw = (tf != null) ? tf.getText() : "";
            long val;

            if (raw == null || raw.isBlank()) {
                val = 0L; inputsSnapshot[i] = "0";
            } else {
                try {
                    long parsed = Long.parseLong(raw.trim());
                    if (parsed < 0) { showError("Invalid input", "Input for " + name + " must be an integer ≥ 0."); return; }
                    val = parsed; inputsSnapshot[i] = raw.trim();
                } catch (NumberFormatException ex) {
                    showError("Invalid input", "Input for " + name + " must be an integer ≥ 0."); return;
                }
            }
            values.add(val);
        }

        long result;
        try {
            Long[] arr = values.toArray(new Long[0]);
            result = executor.run(arr);
        } catch (Exception ex) {
            showError("Execution failed", (ex.getMessage() != null) ? ex.getMessage() : ex.toString());
            return;
        }

        renderVariableState();

        // היסטוריה – רק אם addToHistory=true (ב-ReRun זה false)
        if (addToHistory && app != null) {
            RunRecord rec = new RunRecord();
            int runNo   = app.nextRunNumber();
            int degree  = app.getCurrentExpandLevel();
            long cycles = program.calculateCycle();
            var state   = executor.VariableState();

            rec.record(runNo,
                    program.getName(),
                    app.getLastSourcePath(),
                    degree,
                    inputsSnapshot,
                    result,
                    cycles,
                    state);

            app.addRunRecord(rec); // ידחוף גם לטבלת הימין
        }

        showInfo("Execution Completed", "Result (Y): " + result);
        addToHistory = true; // החזרה לברירת מחדל אחרי ריצה
    }

    // --- SHOW: לפי ה-selected בטבלת ההיסטוריה הימנית ---
    private void onShowClicked() {
        RunRecord rec = (tblRunHistoryRight != null) ? tblRunHistoryRight.getSelectionModel().getSelectedItem() : null;
        if (rec == null && app != null) rec = app.getLastRunRecord();
        if (rec == null) { showInfo("History", "No runs yet."); return; }

        StringBuilder sb = new StringBuilder();
        sb.append("Run #").append(rec.getRunNumber()).append("\n");
        sb.append("Program: ").append(rec.getProgramName()).append("\n");
        sb.append("Path: ").append(rec.getPath() != null ? rec.getPath() : "(unknown)").append("\n");
        sb.append("Degree: ").append(rec.getDegree()).append("\n");
        sb.append("Inputs: ").append(Arrays.toString(rec.getInputs())).append("\n");
        sb.append("Y (result): ").append(rec.getyFinal()).append("\n");
        sb.append("Cycles: ").append(rec.getCycles()).append("\n");
        sb.append("\nVariables State:\n");

        Map<Variable, Long> st = rec.getProgramVariableState();
        if (st != null && !st.isEmpty()) {
            st.forEach((k, v) -> sb.append("  ").append(k.getRepresentation()).append(" = ").append(v).append("\n"));
        } else {
            sb.append("  (empty)\n");
        }

        showInfo("Run Details", sb.toString());
    }

    // --- RE-RUN: לפי ה-selected בטבלת ההיסטוריה הימנית ---
    private void onRerunClicked() {
        if (app == null) return;
        RunRecord rec = (tblRunHistoryRight != null) ? tblRunHistoryRight.getSelectionModel().getSelectedItem() : null;
        if (rec == null) { showInfo("Re-Run", "Select a run from the table."); return; }
        app.rerunRecord(rec);
    }

    // API פנימי ש־AppController קורא אחרי טעינה מחדש:
    public void fillInputs(String[] inputs) {
        if (inputs == null || inputOrder == null) return;
        for (int i = 0; i < Math.min(inputs.length, inputOrder.size()); i++) {
            String name = inputOrder.get(i);
            TextField tf = inputFields.get(name);
            if (tf != null) tf.setText(inputs[i]);
        }
    }
    public void triggerStartWithoutHistory() {
        this.addToHistory = false;
        onStartClicked();
    }

    // הוספה לטבלת היסטוריה הימנית
    public void addRunRecord(RunRecord rec) {
        runRows.add(rec);
        if (tblRunHistoryRight != null) {
            tblRunHistoryRight.getSelectionModel().select(rec);
            tblRunHistoryRight.scrollTo(rec);
        }
    }

    // STATE
    private void renderVariableState() {
        if (varsBox == null || executor == null) return;
        Map<semulator.logic.variable.Variable, Long> map = executor.VariableState();

        Platform.runLater(() -> {
            varsBox.setManaged(true);
            varsBox.setVisible(true);

            varsBox.getChildren().clear();

            boolean has = (map != null && !map.isEmpty());
            if (lblVarsTitle != null) lblVarsTitle.setVisible(has);
            if (!has) { varsBox.requestLayout(); return; }

            for (var e : map.entrySet()) {
                String name  = (e.getKey()  != null) ? e.getKey().getRepresentation() : "(null)";
                String value = (e.getValue()!= null) ? String.valueOf(e.getValue())   : "0";

                Label lName = new Label(name);
                lName.getStyleClass().add("rightbar-state-name");

                Label lVal  = new Label(value);
                lVal.getStyleClass().add("rightbar-state-value");

                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(8, lName, spacer, lVal);
                row.getStyleClass().add("rightbar-state-row");
                varsBox.getChildren().add(row);
            }
            varsBox.requestLayout();
        });
    }

    private void updateCyclesTitle() {
        int total = (program != null) ? program.calculateCycle() : 0;
        if (lblCycles != null) lblCycles.setText("Cycles: " + total);
    }

    private void wireFlash(Button b) {
        if (b == null) return;
        b.setOnAction(e -> { flash(b); /* לוגיקה העיקרית מחוברת ידנית לכפתורים */ });
    }
    private void flash(Node n) {
        if (n == null) return;
        if (!n.getStyleClass().contains("flash-active")) n.getStyleClass().add("flash-active");
        PauseTransition pt = new PauseTransition(Duration.seconds(1));
        pt.setOnFinished(ev -> n.getStyleClass().remove("flash-active"));
        pt.play();
    }

    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(header); a.setContentText(content);
        decorateDialog(a); a.showAndWait();
    }
    private void showInfo(String header, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info"); a.setHeaderText(header); a.setContentText(content);
        decorateDialog(a); a.showAndWait();
    }
    private void decorateDialog(Dialog<?> dlg) {
        try {
            dlg.getDialogPane().getStylesheets().add(
                    getClass().getResource("/semulator/userInterface/mainBar/dialogs-dark.css").toExternalForm()
            );
            dlg.getDialogPane().getStyleClass().add("app-dialog");
        } catch (Exception ignore) { }
    }
}
