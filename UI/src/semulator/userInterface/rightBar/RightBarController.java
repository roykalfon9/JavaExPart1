package semulator.userInterface.rightBar;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import semulator.display.RunRecord;
import semulator.logic.execution.ProgramDebuggerImple;
import semulator.logic.execution.ProgramExecutorImpl;
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;
import semulator.userInterface.mainBar.AppController;

import java.util.*;

public class RightBarController {

    // --- App wiring ---
    private AppController app;
    public void setAppController(AppController app) { this.app = app; }

    // --- Model ---
    private Sprogram program;
    private String  debugCssUrl;

    // --- EXECUTE ---
    private ProgramExecutorImpl executor;

    // --- DEBUG ---
    private ProgramDebuggerImple debugger;
    private boolean debugActive = false;
    private int lastHighlightedInstruction = -1;
    private Set<String> lastHighlightedVars = Collections.emptySet();

    // --- Inputs ---
    private final Map<String, TextField> inputFields = new LinkedHashMap<>();
    private List<String> inputOrder = new ArrayList<>();

    // --- UI refs ---
    @FXML private ToggleGroup modeGroup; // EXECUTE / DEBUG (AccessibleText)
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Button btnStepOver;
    @FXML private Button btnShow;
    @FXML private Button btnRerun;

    @FXML private Label lblCycles;
    @FXML private VBox  inputsBox;
    @FXML private VBox  varsBox;
    @FXML private Label lblVarsTitle;

    // history table (right)
    @FXML private TableView<RunRecord>          tblRunHistoryRight;
    @FXML private TableColumn<RunRecord,Number> colRunIndexRight;
    @FXML private TableColumn<RunRecord,String> colRunProgramRight;
    private final javafx.collections.ObservableList<RunRecord> runRows =
            javafx.collections.FXCollections.observableArrayList();

    private boolean addToHistory = true;

    @FXML
    private void initialize() {
        // flash effect
        attachFlash(btnStart);
        attachFlash(btnStop);
        attachFlash(btnStepOver);
        attachFlash(btnShow);
        attachFlash(btnRerun);

        // handlers
        if (btnStart    != null) btnStart.setOnAction(e -> onStartClicked());
        if (btnStop     != null) btnStop.setOnAction(e -> onStopClicked());
        if (btnStepOver != null) btnStepOver.setOnAction(e -> onStepOverClicked());
        if (btnShow     != null) btnShow.setOnAction(e -> onShowClicked());
        if (btnRerun    != null) btnRerun.setOnAction(e -> onRerunClicked());

        // history table
        if (tblRunHistoryRight != null) tblRunHistoryRight.setItems(runRows);
        if (colRunIndexRight   != null) colRunIndexRight.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().getRunNumber()));
        if (colRunProgramRight != null) colRunProgramRight.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getProgramName()));

        // mode CSS / availability
        if (inputsBox != null) {
            inputsBox.sceneProperty().addListener((obs, o, n) -> { if (n != null) applyMode(); });
        }
        if (modeGroup != null) {
            modeGroup.selectedToggleProperty().addListener((obs, o, n) -> applyMode());
        }
        Platform.runLater(this::applyMode);

        updateCyclesTitle();
        resetDebugUIState();
    }

    // called from AppController after load/expand
    public void bindProgram(Sprogram program) {
        this.program = program;
        rebuildInputsFromProgram();
        updateCyclesTitle();
        resetDebugUIState();
    }

    // ---------- Mode ----------
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

    private void applyMode() {
        var scene = (btnStart != null) ? btnStart.getScene() : null;
        if (scene == null) return;

        // add debug theme sheet once
        if (debugCssUrl == null) {
            debugCssUrl = getClass()
                    .getResource("/semulator/userInterface/mainBar/theme-debug.css")
                    .toExternalForm();
        }
        if (!scene.getStylesheets().contains(debugCssUrl)) {
            scene.getStylesheets().add(debugCssUrl);
        }
        var rootClasses = scene.getRoot().getStyleClass();

        boolean debugMode = "DEBUG".equalsIgnoreCase(currentMode());
        if (debugMode) {
            if (!rootClasses.contains("debug-theme")) rootClasses.add("debug-theme");
        } else {
            rootClasses.remove("debug-theme");
        }

        if (debugMode) {
            // allowlist of buttons that should stay enabled in DEBUG
            Set<String> allow = Set.of("btnStart", "btnStop", "btnStepOver", "LoadButtonTB");

            for (Node n : scene.getRoot().lookupAll(".button")) {
                if (n instanceof ToggleButton tb) {
                    // mode toggles remain enabled always
                    if (modeGroup != null && modeGroup.getToggles().contains(tb)) {
                        if (!tb.disableProperty().isBound()) tb.setDisable(false);
                        continue;
                    }
                }
                if (n instanceof Button b) {
                    String id = b.getId();
                    boolean allowed = (id != null && allow.contains(id));
                    if (!b.disableProperty().isBound()) {
                        b.setDisable(!allowed);
                    }
                }
            }
            if (!debugActive) {
                if (btnStart    != null && !btnStart.disableProperty().isBound())    btnStart.setDisable(false);
                if (btnStop     != null && !btnStop.disableProperty().isBound())     btnStop.setDisable(true);
                if (btnStepOver != null && !btnStepOver.disableProperty().isBound()) btnStepOver.setDisable(true);
            } else {
                if (btnStart    != null && !btnStart.disableProperty().isBound())    btnStart.setDisable(true);
                if (btnStop     != null && !btnStop.disableProperty().isBound())     btnStop.setDisable(false);
                if (btnStepOver != null && !btnStepOver.disableProperty().isBound()) btnStepOver.setDisable(false);
            }
        } else {
            for (Node n : scene.getRoot().lookupAll(".button")) {
                if (n instanceof Button b) {
                    if (!b.disableProperty().isBound()) b.setDisable(false);
                } else if (n instanceof ToggleButton tb) {
                    if (!tb.disableProperty().isBound()) tb.setDisable(false);
                }
            }
            if (btnStop     != null && !btnStop.disableProperty().isBound())     btnStop.setDisable(true);
            if (btnStepOver != null && !btnStepOver.disableProperty().isBound()) btnStepOver.setDisable(true);
            if (btnStart    != null && !btnStart.disableProperty().isBound())    btnStart.setDisable(false);
            resetDebugUIState();
        }
    }

    // ---------- Inputs pane ----------
    private void rebuildInputsFromProgram() {
        inputFields.clear();
        inputOrder.clear();
        if (inputsBox != null) inputsBox.getChildren().clear();
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
            if (inputsBox != null) inputsBox.getChildren().add(row);

            inputFields.put(varName, tf);
        }

        if (varsBox != null) varsBox.getChildren().clear();
        if (lblVarsTitle != null) lblVarsTitle.setVisible(false);
        lastHighlightedVars = Collections.emptySet();
    }

    // ---------- Start ----------
    private void onStartClicked() {
        if ("DEBUG".equalsIgnoreCase(currentMode())) {
            startDebug();
        } else {
            startExecute();
        }
    }

    // EXECUTE
    private void startExecute() {
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

        renderVariableState(executor.VariableState(), Collections.emptySet());

        // עדכון מיידי של התווית "Cycles" לאחר ריצה ב־EXECUTE
        updateCyclesTitle();

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

            app.addRunRecord(rec);
        }

        showInfo("Execution Completed", "Result (Y): " + result);
        addToHistory = true;
    }

    // DEBUG
    private void startDebug() {
        if (program == null) {
            showError("No program", "Please load a program first.");
            return;
        }

        debugger = new ProgramDebuggerImple(program);

        List<Long> values = new ArrayList<>(inputOrder.size());
        for (int i = 0; i < inputOrder.size(); i++) {
            String name = inputOrder.get(i);
            TextField tf = inputFields.get(name);
            String raw = (tf != null) ? tf.getText() : "";
            long val;
            if (raw == null || raw.isBlank()) {
                val = 0L;
            } else {
                try {
                    long parsed = Long.parseLong(raw.trim());
                    if (parsed < 0) { showError("Invalid input", "Input for " + name + " must be an integer ≥ 0."); return; }
                    val = parsed;
                } catch (NumberFormatException ex) {
                    showError("Invalid input", "Input for " + name + " must be an integer ≥ 0."); return;
                }
            }
            values.add(val);
        }

        Long[] arr = values.toArray(new Long[0]);
        debugger.run(arr);

        renderVariableState(debugger.VariableState(), Collections.emptySet());

        debugActive = true;
        applyMode();

        if (app != null && app.getLeftBarController() != null) {
            app.getLeftBarController().showProgramNow();
        }

        lastHighlightedInstruction = -1;
        lastHighlightedVars = Collections.emptySet();
    }

    // STEP OVER
    private void onStepOverClicked() {
        if (!"DEBUG".equalsIgnoreCase(currentMode()) || debugger == null) return;

        debugger.stepOver();

        // חדש: לעדכן את כיתוב ה־Cycles בכל צעד DEBUG
        updateCyclesTitle();

        int idx = debugger.getInstructionIndex();
        if (app != null && app.getLeftBarController() != null) {
            app.getLeftBarController().highlightInstructionByIndex(idx);
        }
        lastHighlightedInstruction = idx;

        Set<String> hl = new LinkedHashSet<>();
        Variable v1 = debugger.getLastVariableChange();
        if (v1 != null) hl.add(v1.getRepresentation());

        renderVariableState(debugger.VariableState(), hl);
        lastHighlightedVars = hl;

        if (debugger.isOver()) {
            finishDebugRun();
        }
    }

    // STOP -> run to end
    private void onStopClicked() {
        if (!"DEBUG".equalsIgnoreCase(currentMode()) || debugger == null) return;
        while (!debugger.isOver()) {
            debugger.stepOver();
        }
        renderVariableState(debugger.VariableState(), Collections.emptySet());

        // בסיום ריצה בדיבאג לעדכן גם כאן
        updateCyclesTitle();

        finishDebugRun();
    }

    private void finishDebugRun() {
        long result = (debugger != null && debugger.getResult() != null) ? debugger.getResult() : 0L;

        if (addToHistory && app != null) {
            RunRecord rec = new RunRecord();
            int runNo   = app.nextRunNumber();
            int degree  = app.getCurrentExpandLevel();
            long cycles = program.calculateCycle();
            var state   = debugger.VariableState();

            String[] inputsSnapshot = new String[inputOrder.size()];
            for (int i = 0; i < inputOrder.size(); i++) {
                String name = inputOrder.get(i);
                String raw = (inputFields.get(name) != null) ? inputFields.get(name).getText() : "";
                inputsSnapshot[i] = (raw == null || raw.isBlank()) ? "0" : raw.trim();
            }

            rec.record(runNo,
                    program.getName(),
                    app.getLastSourcePath(),
                    degree,
                    inputsSnapshot,
                    result,
                    cycles,
                    state);

            app.addRunRecord(rec);
        }

        showInfo("Debug Finished", "Result (Y): " + result);

        debugActive = false;
        debugger = null;
        lastHighlightedInstruction = -1;
        lastHighlightedVars = Collections.emptySet();

        applyMode();
    }

    // SHOW run details
    private void onShowClicked() {
        RunRecord rec = (tblRunHistoryRight != null)
                ? tblRunHistoryRight.getSelectionModel().getSelectedItem()
                : null;
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

    // Re-run from selected record
    private void onRerunClicked() {
        if (app == null) return;
        RunRecord rec = (tblRunHistoryRight != null)
                ? tblRunHistoryRight.getSelectionModel().getSelectedItem()
                : null;
        if (rec == null) { showInfo("Re-Run", "Select a run from the table."); return; }
        app.rerunRecord(rec);
    }

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
        startExecute();
        this.addToHistory = true;
    }

    public void addRunRecord(RunRecord rec) {
        runRows.add(rec);
        if (tblRunHistoryRight != null) {
            tblRunHistoryRight.getSelectionModel().select(rec);
            tblRunHistoryRight.scrollTo(rec);
        }
    }

    private void renderVariableState(Map<Variable, Long> state, Set<String> namesToHighlight) {
        if (varsBox == null) return;

        Platform.runLater(() -> {
            varsBox.setManaged(true);
            varsBox.setVisible(true);
            varsBox.getChildren().clear();

            boolean has = (state != null && !state.isEmpty());
            if (lblVarsTitle != null) lblVarsTitle.setVisible(has);
            if (!has) { varsBox.requestLayout(); return; }

            for (var e : state.entrySet()) {
                String name  = (e.getKey()  != null) ? e.getKey().getRepresentation() : "(null)";
                String value = (e.getValue()!= null) ? String.valueOf(e.getValue())   : "0";

                Label lName = new Label(name);
                lName.getStyleClass().add("rightbar-state-name");

                Label lVal  = new Label(value);
                lVal.getStyleClass().add("rightbar-state-value");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(8, lName, spacer, lVal);
                row.getStyleClass().add("rightbar-state-row");

                if (namesToHighlight != null && namesToHighlight.contains(name)) {
                    row.getStyleClass().add("state-changed");
                }

                varsBox.getChildren().add(row);
            }
            varsBox.requestLayout();
        });
    }

    private void updateCyclesTitle() {
        int total = (program != null) ? program.calculateCycle() : 0;
        if (lblCycles != null) lblCycles.setText("Cycles: " + total);
    }

    // add flash without overriding existing onAction
    private void attachFlash(Button b) {
        if (b == null) return;
        b.addEventHandler(ActionEvent.ACTION, e -> flash(b));
    }

    private void flash(Node n) {
        if (n == null) return;
        if (!n.getStyleClass().contains("flash-active")) n.getStyleClass().add("flash-active");
        PauseTransition pt = new PauseTransition(Duration.seconds(1));
        pt.setOnFinished(ev -> n.getStyleClass().remove("flash-active"));
        pt.play();
    }

    // dialogs
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

    private void resetDebugUIState() {
        debugActive = false;
        debugger = null;
        lastHighlightedInstruction = -1;
        lastHighlightedVars = Collections.emptySet();
        if (btnStop     != null && !btnStop.disableProperty().isBound())     btnStop.setDisable(true);
        if (btnStepOver != null && !btnStepOver.disableProperty().isBound()) btnStepOver.setDisable(true);
        if (btnStart    != null && !btnStart.disableProperty().isBound())    btnStart.setDisable(false);
    }

    // shim (avoid import name clash w/ Region)
    private static class Region extends javafx.scene.layout.Region {}
}
