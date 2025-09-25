package semulator.userInterface.rightBar;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import semulator.logic.execution.ProgramExecutorImpl;
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;

import java.util.*;

public class RightBarController {

    // --- מודל/מצב ---
    private Sprogram program;
    private ProgramExecutorImpl executor;
    private final Map<String, TextField> inputFields = new LinkedHashMap<>();
    private List<String> inputOrder = new ArrayList<>();

    // --- עמודת STATE (שמאל בתוך הסקרול) — CHANGED ---
    @FXML private VBox varsBox;          // ← שים fx:id="varsBox" בעמודה השמאלית ב-FXML
    @FXML private Label lblVarsTitle;    // ← אופציונלי: אם קיימת כותרת סטטית ב-FXML

    // --- טוגלים למצב עבודה ---
    @FXML private ToggleGroup modeGroup;

    // --- כפתורים ---
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Button btnStepOver;
    @FXML private Button btnShow;
    @FXML private Button btnRerun;

    @FXML private Label lblCycles;

    // --- עמודת האינפוטים (ימין בתוך הסקרול) ---
    @FXML private VBox inputsBox;

    @FXML
    private void initialize() {
        wireFlash(btnStart);
        wireFlash(btnStop);
        wireFlash(btnStepOver);
        wireFlash(btnShow);
        wireFlash(btnRerun);

        if (btnStart != null) {
            btnStart.setOnAction(e -> onStartClicked());
        }

        inputsBox.sceneProperty().addListener((obs, o, n) -> { if (n != null) applyMode(); });
        if (modeGroup != null) {
            modeGroup.selectedToggleProperty().addListener((obs, o, n) -> applyMode());
        }
        Platform.runLater(this::applyMode);

        updateCyclesTitle();
        clearStateView(); // ← נתחיל בלי STATE מוצג
    }

    public void bindProgram(Sprogram program) {
        this.program = program;
        rebuildInputsFromProgram(); // ← יבנה את שדות האינפוטים בלבד (ימין)
        updateCyclesTitle();
        clearStateView();           // ← איפוס עמודת STATE בכל טעינה/הרחבה
    }

    // --- מצב הטוגלים ---
    private String currentMode() {
        if (modeGroup == null || modeGroup.getSelectedToggle() == null) return "EXECUTE";
        return readModeFromToggle(modeGroup.getSelectedToggle());
    }
    private String readModeFromToggle(Toggle t) {
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

    // --- תימאט-DEBUG גלובלית + זמינות כפתורים ---
    private String debugCssUrl;
    private void applyMode() {
        var scene = (btnStart != null) ? btnStart.getScene() : null;
        if (scene == null) return;

        if (debugCssUrl == null) {
            debugCssUrl = getClass().getResource("/semulator/userInterface/mainBar/theme-debug.css").toExternalForm();
        }
        boolean debug = "DEBUG".equalsIgnoreCase(currentMode());
        if (!scene.getStylesheets().contains(debugCssUrl)) scene.getStylesheets().add(debugCssUrl);
        var rootClasses = scene.getRoot().getStyleClass();
        if (debug) {
            if (!rootClasses.contains("debug-theme")) rootClasses.add("debug-theme");
        } else {
            rootClasses.remove("debug-theme");
        }

        if (btnStop != null)     btnStop.setDisable(!debug);
        if (btnStepOver != null) btnStepOver.setDisable(!debug);

        if (debug) {
            Set<String> allow = new HashSet<>(Arrays.asList("btnStart","btnStop","btnStepOver","LoadButtonTB"));
            for (Node n : scene.getRoot().lookupAll(".button")) {
                if (n instanceof Button b) b.setDisable(!(b.getId()!=null && allow.contains(b.getId())));
            }
            if (modeGroup != null) {
                for (Toggle t : modeGroup.getToggles()) if (t instanceof ToggleButton tb) tb.setDisable(false);
            }
        } else {
            for (Node n : scene.getRoot().lookupAll(".button")) if (n instanceof Button b) b.setDisable(false);
            if (btnStop != null)     btnStop.setDisable(true);
            if (btnStepOver != null) btnStepOver.setDisable(true);
        }
    }

    // --- בניית שדות קלט (ימין) — CHANGED: בלי הוספת STATE לכאן! ---
    private void rebuildInputsFromProgram() {
        inputFields.clear();
        inputOrder.clear();
        inputsBox.getChildren().clear();

        if (program == null) return;

        executor = new ProgramExecutorImpl(program);
        inputOrder = Optional.ofNullable(executor.getInputLabelsNames()).orElseGet(ArrayList::new);

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
    }

    // --- EXECUTE: Start ---
    private void onStartClicked() {
        if (!"EXECUTE".equalsIgnoreCase(currentMode())) return;
        if (executor == null) { showError("No program", "Please load a program first."); return; }

        List<Long> values = new ArrayList<>(inputOrder.size());
        for (String name : inputOrder) {
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

        long result;
        try {
            result = executor.run(values.toArray(new Long[0]));
        } catch (Exception ex) {
            showError("Execution failed", (ex.getMessage()!=null)? ex.getMessage(): ex.toString());
            return;
        }

        showInfo("Execution Completed", "Result (Y): " + result);

        // מציגים את מצב המשתנים בעמודה השמאלית (varsBox)
        renderVariableState(); // ← CHANGED: ממלא את varsBox ולא את inputsBox
    }

    // --- ניקוי עמודת STATE (שמאל) — CHANGED ---
    private void clearStateView() {
        if (varsBox != null) varsBox.getChildren().clear();
        if (lblVarsTitle != null) lblVarsTitle.setVisible(false);
    }

    // --- מילוי עמודת STATE (שמאל) — CHANGED ---
    private void renderVariableState() {
        if (varsBox == null || executor == null) return;

        Map<semulator.logic.variable.Variable, Long> map = executor.VariableState();

        // עדכוני UI רק על FX Thread
        Platform.runLater(() -> {
            varsBox.setManaged(true);
            varsBox.setVisible(true);

            varsBox.getChildren().clear();

            boolean has = (map != null && !map.isEmpty());
            if (lblVarsTitle != null) lblVarsTitle.setVisible(has);
            if (!has) {
                varsBox.requestLayout();
                return;
            }

            // אם אין Label כותרת ב-FXML, נדאג לו כאן (לא יפריע אם יש כבר):
            if (lblVarsTitle == null) {
                Label title = new Label("State");
                title.getStyleClass().add("title");
                varsBox.getChildren().add(title);
            }

            for (var e : map.entrySet()) {
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
                varsBox.getChildren().add(row);
            }

            varsBox.requestLayout();
        });
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        r.setPrefHeight(h);
        return r;
    }

    // --- אפקט הבזק קצר ---
    private void wireFlash(Button b) {
        if (b == null) return;
        b.setOnAction(e -> flash(b));
    }
    private void flash(Node n) {
        if (n == null) return;
        if (!n.getStyleClass().contains("flash-active")) n.getStyleClass().add("flash-active");
        PauseTransition pt = new PauseTransition(Duration.seconds(1));
        pt.setOnFinished(ev -> n.getStyleClass().remove("flash-active"));
        pt.play();
    }

    // --- דיאלוגים ---
    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(content);
        decorateDialog(a);
        a.showAndWait();
    }
    private void showInfo(String header, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(header);
        a.setContentText(content);
        decorateDialog(a);
        a.showAndWait();
    }
    private void decorateDialog(Dialog<?> dlg) {
        try {
            dlg.getDialogPane().getStylesheets().add(
                    getClass().getResource("/semulator/userInterface/mainBar/dialogs-dark.css").toExternalForm()
            );
            dlg.getDialogPane().getStyleClass().add("app-dialog");
        } catch (Exception ignore) { }
    }

    // --- כותרת הסייקלים ---
    private void updateCyclesTitle() {
        int total = (program != null) ? program.calculateCycle() : 0;
        if (lblCycles != null) lblCycles.setText("Cycles: " + total);
    }
}
