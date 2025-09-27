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
import semulator.logic.execution.ProgramDebuggerImple; // ← דיבאג
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;
import semulator.userInterface.mainBar.AppController;

import java.util.*;

public class RightBarController {

    // --- חיבור לאפליקציה ---
    private AppController app;
    public void setAppController(AppController app) { this.app = app; }

    // --- מצב/מודל כללי ---
    private Sprogram program;                      // התכנית הנוכחית (מקור/מורחבת)
    private String  debugCssUrl;                   // תמת DEBUG (צהוב)

    // --- EXECUTE ---
    private ProgramExecutorImpl executor;          // מריץ מלא

    // --- DEBUG ---
    private ProgramDebuggerImple debugger;         // מדבג צעד-אחר-צעד
    private boolean debugActive = false;           // האם דיבאג החל (אחרי Start)
    private int      lastHighlightedInstruction = -1;  // הדגשת פקודה בטבלה העליונה (שמאל)
    private Set<String> lastHighlightedVars = Collections.emptySet(); // הדגשת משתנים ב-Vars

    // --- קלט מהמשתמש ---
    private final Map<String, TextField> inputFields = new LinkedHashMap<>(); // שם משתנה -> TextField
    private List<String> inputOrder = new ArrayList<>();                       // סדר קלטים

    // --- כפתורים/טוגלים/תצוגה ---
    @FXML private ToggleGroup modeGroup; // AccessibleText: EXECUTE / DEBUG
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Button btnStepOver;
    @FXML private Button btnShow;     // מציג פרטי ריצה (מהטבלה הימנית)
    @FXML private Button btnRerun;    // Re-Run מרשומה נבחרת

    @FXML private Label  lblCycles;
    @FXML private VBox   inputsBox;   // HBox-ים של קלטים (שמאל של אזור הסקרול)
    @FXML private VBox   varsBox;     // HBox-ים של מצב משתנים (ימין של אזור הסקרול)
    @FXML private Label  lblVarsTitle;

    // --- טבלת היסטוריה הימנית (2 עמודות: #ריצה, שם תכנית) ---
    @FXML private TableView<RunRecord>           tblRunHistoryRight;
    @FXML private TableColumn<RunRecord,Number>  colRunIndexRight;
    @FXML private TableColumn<RunRecord,String>  colRunProgramRight;
    private final javafx.collections.ObservableList<RunRecord> runRows =
            javafx.collections.FXCollections.observableArrayList();

    // --- דגל: האם להוסיף היסטוריה (ב-ReRun לא) ---
    private boolean addToHistory = true;

    @FXML
    private void initialize() {
        // אפקט "הבזק" קטן לכל הכפתורים
        wireFlash(btnStart);
        wireFlash(btnStop);
        wireFlash(btnStepOver);
        wireFlash(btnShow);
        wireFlash(btnRerun);

        // חיבורים פונקציונליים
        if (btnStart    != null) btnStart.setOnAction(e -> onStartClicked());
        if (btnStop     != null) btnStop.setOnAction(e -> onStopClicked());
        if (btnStepOver != null) btnStepOver.setOnAction(e -> onStepOverClicked());
        if (btnShow     != null) btnShow.setOnAction(e -> onShowClicked());
        if (btnRerun    != null) btnRerun.setOnAction(e -> onRerunClicked());

        // טבלת היסטוריה ימנית
        if (tblRunHistoryRight != null) tblRunHistoryRight.setItems(runRows);
        if (colRunIndexRight   != null) colRunIndexRight.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().getRunNumber()));
        if (colRunProgramRight != null) colRunProgramRight.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getProgramName()));

        // החלת מצב (EXECUTE/DEBUG) כשה-Scene מצורף
        if (inputsBox != null) {
            inputsBox.sceneProperty().addListener((obs, o, n) -> { if (n != null) applyMode(); });
        }
        if (modeGroup != null) {
            modeGroup.selectedToggleProperty().addListener((obs, o, n) -> applyMode());
        }
        Platform.runLater(this::applyMode);

        updateCyclesTitle();
        resetDebugUIState(); // ודא שמצב דיבאג כבוי בתחילת הדרך
    }

    // יקרא מה-AppController אחרי טעינה/הרחבה
    public void bindProgram(Sprogram program) {
        this.program = program;
        rebuildInputsFromProgram();
        updateCyclesTitle();
        resetDebugUIState(); // בכל החלפת תכנית – נבטל דיבאג פעיל
    }

    // --- מצב (EXECUTE/DEBUG) ---
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

    // תמה/זמינות כפתורים בכל מעבר מצב
    private void applyMode() {
        var scene = (btnStart != null) ? btnStart.getScene() : null;
        if (scene == null) return;

        // תמת DEBUG (צהוב) – נוסיף תמיד לרשימת ה-CSS, אך השליטה נעשית ע״י class בשורש
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

        // השבתת כל הכפתורים במצב DEBUG למעט Start/Stop/StepOver בימין + LoadButtonTB בטופ-בר
        if (debugMode) {
            Set<String> allow = new HashSet<>();
            allow.add("btnStart");
            allow.add("btnStop");
            allow.add("btnStepOver");
            allow.add("LoadButtonTB");

            for (Node n : scene.getRoot().lookupAll(".button")) {
                if (n instanceof ToggleButton tb) {
                    // טוגלי מצב נשארים פעילים תמיד
                    if (modeGroup != null && modeGroup.getToggles().contains(tb)) {
                        tb.setDisable(false);
                        continue;
                    }
                }
                if (n instanceof Button b) {
                    String id = b.getId();
                    boolean allowed = (id != null && allow.contains(id));
                    b.setDisable(!allowed);
                }
            }
            // זמינות פנימית (בימין)
            if (!debugActive) { // לפני לחיצה על Start בדיבאג
                if (btnStart    != null) btnStart.setDisable(false);
                if (btnStop     != null) btnStop.setDisable(true);
                if (btnStepOver != null) btnStepOver.setDisable(true);
            } else {             // אחרי Start בדיבאג
                if (btnStart    != null) btnStart.setDisable(true);
                if (btnStop     != null) btnStop.setDisable(false);
                if (btnStepOver != null) btnStepOver.setDisable(false);
            }
        } else {
            // EXECUTE: שחרר הכל, ואז אמץ חוקים ל-EXECUTE
            for (Node n : scene.getRoot().lookupAll(".button")) {
                if (n instanceof Button b) b.setDisable(false);
                else if (n instanceof ToggleButton tb) tb.setDisable(false);
            }
            if (btnStop     != null) btnStop.setDisable(true);
            if (btnStepOver != null) btnStepOver.setDisable(true);
            if (btnStart    != null) btnStart.setDisable(false);
            resetDebugUIState();
        }
    }

    // --- בניית אזור קלטים דינאמי ---
    private void rebuildInputsFromProgram() {
        inputFields.clear();
        inputOrder.clear();
        inputsBox.getChildren().clear();

        if (program == null) return;

        // יוצר Executor חדש מהתכנית הנוכחית לקבלת סדר קלטים
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

        // נקה STATE
        if (varsBox != null) varsBox.getChildren().clear();
        if (lblVarsTitle != null) lblVarsTitle.setVisible(false);
        lastHighlightedVars = Collections.emptySet();
    }

    // --- START: EXECUTE או DEBUG לפי מצב ---
    private void onStartClicked() {
        if ("DEBUG".equalsIgnoreCase(currentMode())) {
            startDebug();
        } else {
            startExecute();
        }
    }

    // EXECUTE מלא
    private void startExecute() {
        if (executor == null || program == null) {
            showError("No program", "Please load a program first.");
            return;
        }

        // קריאת קלטים
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

        // הרצה
        long result;
        try {
            Long[] arr = values.toArray(new Long[0]);
            result = executor.run(arr);
        } catch (Exception ex) {
            showError("Execution failed", (ex.getMessage() != null) ? ex.getMessage() : ex.toString());
            return;
        }

        // הצגת STATE
        renderVariableState(executor.VariableState(), Collections.emptySet());

        // היסטוריה (Start בלבד – לא ReRun)
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
        addToHistory = true; // להבא
    }

    // DEBUG – אתחול דיבאג + הרצת run(...) להגדרת הקלטים, הצגת טבלה עליונה, מצב כפתורים
    private void startDebug() {
        if (program == null) {
            showError("No program", "Please load a program first.");
            return;
        }

        debugger = new ProgramDebuggerImple(program);

        // לקרוא קלטים – כמו ב-EXECUTE (ברירת מחדל: 0)
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

        // אתחול קונטקסט וקלטים בדיבאג
        Long[] arr = values.toArray(new Long[0]);
        debugger.run(arr);

        // הצגת STATE ראשוני
        renderVariableState(debugger.VariableState(), Collections.emptySet());

        // הדלקת דיבאג – כפתורים
        debugActive = true;
        applyMode();

        // בקשה מהשמאלי להציג מייד את הטבלה העליונה (Show Program)
        if (app != null && app.getLeftBarController() != null) {
            app.getLeftBarController().showProgramNow();
        }

        // ניקוי הדגשות קודמות
        lastHighlightedInstruction = -1;
        lastHighlightedVars = Collections.emptySet();
    }

    // STEP OVER – מבצע צעד, מדגיש פקודה ומשתנים, ובודק סיום
    private void onStepOverClicked() {
        if (!"DEBUG".equalsIgnoreCase(currentMode()) || debugger == null) return;

        debugger.stepOver();

        // הדגשת שורת פקודה בשמאל (לפי instructionIndex)
        int idx = debugger.getInstructionIndex();
        if (app != null && app.getLeftBarController() != null) {
            app.getLeftBarController().highlightInstructionByIndex(idx);
        }
        lastHighlightedInstruction = idx;

        // הדגשת משתנה ששונה בצעד האחרון (אם יש)
        Set<String> hl = new LinkedHashSet<>();
        Variable v1 = debugger.getLastVariableChange(); // ← כרגע רק ראשי (אם תרצה גם שני: הוסף API במחלקת הדיבאג)
        if (v1 != null) hl.add(v1.getRepresentation());

        renderVariableState(debugger.VariableState(), hl);
        lastHighlightedVars = hl;

        // בדיקת סיום – אם הסתיים, סוגרים/מוסיפים להיסטוריה/מודיעים
        if (debugger.isOver()) {
            finishDebugRun();
        }
    }

    // STOP – מריץ צעדים עד סוף ואז מסיים כמו ב-StepOver סופי
    private void onStopClicked() {
        if (!"DEBUG".equalsIgnoreCase(currentMode()) || debugger == null) return;
        while (!debugger.isOver()) {
            debugger.stepOver();
        }
        // עדכון תצוגה אחרון (למקרה שקרו שינויים בצעד האחרון)
        renderVariableState(debugger.VariableState(), Collections.emptySet());
        finishDebugRun();
    }

    // סיום דיבאג: היסטוריה + דיאלוג תוצאה + איפוס מצב כפתורים
    private void finishDebugRun() {
        long result = (debugger != null && debugger.getResult() != null) ? debugger.getResult() : 0L;

        // הכנסת ריצה להיסטוריה (Start בדיבאג = ריצה חדשה)
        if (addToHistory && app != null) {
            RunRecord rec = new RunRecord();
            int runNo   = app.nextRunNumber();
            int degree  = app.getCurrentExpandLevel();
            long cycles = program.calculateCycle();
            var state   = debugger.VariableState();

            // צילום קלטים טקסטואליים מהשדות (לנוחות ה-Show/Rerun)
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

        // איפוס דיבאג
        debugActive = false;
        debugger = null;
        lastHighlightedInstruction = -1;
        lastHighlightedVars = Collections.emptySet();

        // כפתורים לפי מצב DEBUG (Start זמין, Stop/StepOver כבויים)
        applyMode();
    }

    // SHOW – חלון עם פרטי הריצה מהרשומה הנבחרת
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

    // RERUN – טוען ע״פ נתיב הרשומה, ממלא קלטים ומריץ Start ללא היסטוריה
    private void onRerunClicked() {
        if (app == null) return;
        RunRecord rec = (tblRunHistoryRight != null)
                ? tblRunHistoryRight.getSelectionModel().getSelectedItem()
                : null;
        if (rec == null) { showInfo("Re-Run", "Select a run from the table."); return; }
        app.rerunRecord(rec);
    }

    // מילוי קלטים מבחוץ (AppController לאחר טעינה מחדש)
    public void fillInputs(String[] inputs) {
        if (inputs == null || inputOrder == null) return;
        for (int i = 0; i < Math.min(inputs.length, inputOrder.size()); i++) {
            String name = inputOrder.get(i);
            TextField tf = inputFields.get(name);
            if (tf != null) tf.setText(inputs[i]);
        }
    }
    // טריגר ריצה ללא היסטוריה (עבור ReRun)
    public void triggerStartWithoutHistory() {
        this.addToHistory = false;
        startExecute(); // ב-ReRun מריצים כ-Execute מלא
        this.addToHistory = true; // חזרה לברירת מחדל
    }

    // הוספה לטבלת היסטוריה הימנית (נקרא מ-AppController.addRunRecord)
    public void addRunRecord(RunRecord rec) {
        runRows.add(rec);
        if (tblRunHistoryRight != null) {
            tblRunHistoryRight.getSelectionModel().select(rec);
            tblRunHistoryRight.scrollTo(rec);
        }
    }

    // ציור מצב משתנים – עם הדגשות (namesToHighlight)
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

                // הדגשה אם נמצא ברשימת ההדגשה
                if (namesToHighlight != null && namesToHighlight.contains(name)) {
                    row.getStyleClass().add("state-changed");
                }

                varsBox.getChildren().add(row);
            }
            varsBox.requestLayout();
        });
    }

    // עדכון כותרת Cycles
    private void updateCyclesTitle() {
        int total = (program != null) ? program.calculateCycle() : 0;
        if (lblCycles != null) lblCycles.setText("Cycles: " + total);
    }

    // אפקט הבזק קטן (UI בלבד)
    private void wireFlash(Button b) {
        if (b == null) return;
        b.setOnAction(e -> { flash(b); /* לוגיקת הפעולה נקשרת ידנית במתודות */ });
    }
    private void flash(Node n) {
        if (n == null) return;
        if (!n.getStyleClass().contains("flash-active")) n.getStyleClass().add("flash-active");
        PauseTransition pt = new PauseTransition(Duration.seconds(1));
        pt.setOnFinished(ev -> n.getStyleClass().remove("flash-active"));
        pt.play();
    }

    // דיאלוגים מעוצבים
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

    // איפוס מצב דיבאג (כפתורים/דגלים)
    private void resetDebugUIState() {
        debugActive = false;
        debugger = null;
        lastHighlightedInstruction = -1;
        lastHighlightedVars = Collections.emptySet();
        if (btnStop     != null) btnStop.setDisable(true);
        if (btnStepOver != null) btnStepOver.setDisable(true);
        if (btnStart    != null) btnStart.setDisable(false);
    }

    // ---- עזר קטן (ל־CSS) ----
    private static class Region extends javafx.scene.layout.Region {}
}
