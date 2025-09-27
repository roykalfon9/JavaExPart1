package semulator.userInterface.LeftBar;

import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import semulator.logic.api.Sinstruction;
import semulator.logic.program.Sprogram;
import semulator.userInterface.mainBar.AppController;

import java.util.ArrayList;
import java.util.List;

public class LeftBarController {

    // --- חיבור לאפליקציה (נדרש כדי לקרוא apply/show וכו׳) ---
    private AppController app;
    public void setAppController(AppController app) { this.app = app; }

    // --- מודל ---
    private Sprogram program;
    private int currExpandLevel = 0;

    @FXML private HBox toolbar;

    // כפתורי כלים
    @FXML private Button btnProgramSelector;
    @FXML private Button btnCollapse;
    @FXML private Button btnExpand;
    @FXML private Button btnCurrentMaxDegree;
    @FXML private Button btnHighlightSelection;

    // טבלה עליונה – פקודות
    @FXML private TableView<Sinstruction> tblInstructions;
    @FXML private TableColumn<Sinstruction, Number>  colIdx;
    @FXML private TableColumn<Sinstruction, String>  colBS;
    @FXML private TableColumn<Sinstruction, Number>  colCycles;
    @FXML private TableColumn<Sinstruction, String>  colInstruction;

    // לייבל בין הטבלאות
    @FXML private Label lblBetweenTables;

    // טבלה תחתונה – היסטוריית "שרשרת הורים" (Highlight Selection)
    @FXML private TableView<Sinstruction> tblHistory;
    @FXML private TableColumn<Sinstruction, Number> colHIdx;
    @FXML private TableColumn<Sinstruction, String>  colHBS;
    @FXML private TableColumn<Sinstruction, Number> colHCycles;
    @FXML private TableColumn<Sinstruction, String>  colHInstruction;

    private final ObservableList<Sinstruction> rows = FXCollections.observableArrayList();
    private final ObservableList<Sinstruction> historyRows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {

        // אפקט "הבזק" לכל כפתור בשורת הכלים
        toolbar.getChildren().stream()
                .filter(n -> n instanceof Button)
                .map(n -> (Button) n)
                .forEach(b -> b.addEventHandler(ActionEvent.ACTION, e -> flash(b, 1)));

        // חיבורים לפעולות
        btnCurrentMaxDegree.setOnAction(e -> showDegreeDialog());
        btnExpand.setOnAction(e -> onExpandClicked());
        btnCollapse.setOnAction(e -> onCollapseClicked());
        btnHighlightSelection.setOnAction(e -> onHighlightClicked());

        // עמודות לטבלה העליונה
        colIdx.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().getInstructionNumber()));
        colBS.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isBasic()));
        colCycles.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().cycles()));
        colInstruction.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().toDisplayString()));

        // עמודות לטבלה התחתונה (רשימת הורים)
        colHIdx.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().getInstructionNumber()));
        colHBS.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isBasic()));
        colHCycles.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().cycles()));
        colHInstruction.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().toDisplayString()));

        // חיבור רשימות נתונים
        tblInstructions.setItems(rows);
        tblHistory.setItems(historyRows);

        // ניקוי ראשוני
        rows.clear();
        historyRows.clear();
        tblInstructions.setDisable(true);
        lblBetweenTables.setText("");

        // חשוב: כל שינוי בחירה בטבלה העליונה מנקה את הטבלה התחתונה
        tblInstructions.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> {
                    historyRows.clear();
                    tblHistory.getSelectionModel().clearSelection();
                });
    }

    private void flash(Button b, double seconds) {
        if (!b.getStyleClass().contains("flash")) b.getStyleClass().add("flash");
        PauseTransition pt = new PauseTransition(Duration.seconds(seconds));
        pt.setOnFinished(ev -> b.getStyleClass().remove("flash"));
        pt.play();
    }

    // מצמיד תוכנית; showNow=true יציג מייד את הטבלה העליונה
    public void bindProgram(Sprogram program) { bindProgram(program, false); }

    public void bindProgram(Sprogram program, boolean showNow) {
        this.program = program;

        // ניקוי טבלאות בעת הצמדה של תוכנית חדשה
        historyRows.clear();
        rows.clear();

        if (this.program != null) this.program.setNumberInstructions();
        updateCountsLabel();

        if (showNow && this.program != null) {
            rows.setAll(this.program.getInstructions());
            tblInstructions.setDisable(false);
            if (!rows.isEmpty()) tblInstructions.getSelectionModel().select(0);
        } else {
            tblInstructions.setDisable(true);
        }
    }

    // "Show Program" – מציג טבלה עליונה (נקרא מבחוץ בדיבאג Start)
    public void showProgramNow() { onProgramSelection(); } // ← NEW

    // הדגשת שורה לפי אינדקס 0-based (נקרא מבחוץ בדיבאג Step Over)
    public void highlightInstructionByIndex(int zeroBasedIndex) { // ← NEW
        if (tblInstructions.getItems() == null || tblInstructions.getItems().isEmpty()) return;
        if (zeroBasedIndex < 0 || zeroBasedIndex >= tblInstructions.getItems().size()) {
            tblInstructions.getSelectionModel().clearSelection();
            return;
        }
        tblInstructions.getSelectionModel().select(zeroBasedIndex);
        tblInstructions.scrollTo(zeroBasedIndex);
    }

    @FXML
    private void onProgramSelection() {
        if (program == null) return;
        rows.setAll(program.getInstructions());
        tblInstructions.setDisable(false);
        if (!rows.isEmpty()) tblInstructions.getSelectionModel().select(0);
        // בכל הצגה/רענון של העליונה – מנקים תחתונה
        historyRows.clear();
    }

    private void showDegreeDialog() {
        int max = (program != null && app != null) ? app.getMaxDegree() : 0;

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.initStyle(StageStyle.UTILITY);
        a.setTitle("Current / Max Degree");
        a.setHeaderText("Program Degrees");
        a.setContentText(
                "Current expand level: " + currExpandLevel + "\n" +
                        "Max degree (program): " + max
        );

        a.getDialogPane().getStylesheets().add(
                getClass().getResource("/semulator/userInterface/mainBar/dialogs-dark.css").toExternalForm()
        );
        a.getDialogPane().getStyleClass().add("app-dialog");

        a.showAndWait();
    }

    private void onExpandClicked() {
        if (app == null) return;
        currExpandLevel += 1;
        app.requestExpandTo(currExpandLevel, this);
    }

    private void onCollapseClicked() {
        if (app == null) return;
        currExpandLevel -= 1;
        app.requestExpandTo(currExpandLevel, this);
    }

    public void setCurrExpandLevel(int level) { this.currExpandLevel = Math.max(0, level); }
    public int  getCurrExpandLevel() { return currExpandLevel; }
    public void resetExpandLevel() { this.currExpandLevel = 0; }

    // לחיצה על Highlight – שרשרת הורים לתחתונה
    private void onHighlightClicked() {
        Sinstruction sel = tblInstructions.getSelectionModel().getSelectedItem();
        if (sel == null) {
            historyRows.clear();
            return;
        }
        historyRows.setAll(buildParentChain(sel));
        if (!historyRows.isEmpty()) tblHistory.getSelectionModel().select(0);
    }

    // בניית שרשרת הורים (הנבחר -> הורה -> הורה של הורה ...)
    private List<Sinstruction> buildParentChain(Sinstruction leaf) {
        List<Sinstruction> chain = new ArrayList<>();
        Sinstruction cur = leaf;
        while (cur != null) {
            chain.add(cur);
            cur = cur.getParentInstruction();
        }
        return chain;
    }

    // עדכון הלייבל בין הטבלאות – ספירת בסיסיות/סינתטיות
    private void updateCountsLabel() {
        if (program == null) {
            lblBetweenTables.setText("");
            return;
        }
        int basic = program.getBasicInstructionNumber();
        int syn   = program.getSynteticInstructionNumber();
        lblBetweenTables.setText(
                "The number of basic instructions in the program is: " + basic +
                        " | The number of synthetic instructions in the program is: " + syn
        );
    }
}
