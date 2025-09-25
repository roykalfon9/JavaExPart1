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

    private AppController app;
    public void setAppController(AppController app) { this.app = app; }

    private Sprogram program;
    private int currExpandLevel = 0;

    @FXML private HBox toolbar;

    // Top toolbar
    @FXML private Button btnProgramSelector;
    @FXML private Button btnCollapse;
    @FXML private Button btnExpand;
    @FXML private Button btnCurrentMaxDegree;
    @FXML private Button btnHighlightSelection;

    // Upper table
    @FXML private TableView<Sinstruction> tblInstructions;
    @FXML private TableColumn<Sinstruction, Number>  colIdx;
    @FXML private TableColumn<Sinstruction, String>  colBS;
    @FXML private TableColumn<Sinstruction, Number>  colCycles;
    @FXML private TableColumn<Sinstruction, String>  colInstruction;

    // Between tables
    @FXML private Label lblBetweenTables;

    // Lower table (history/chain)
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

        // עמודות לטבלה התחתונה (היסטוריית/שרשרת הורים)  // NEW: הגדרת עמודות הטבלה התחתונה
        colHIdx.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().getInstructionNumber())); // NEW
        colHBS.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isBasic()));               // NEW
        colHCycles.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().cycles()));           // NEW
        colHInstruction.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().toDisplayString())); // NEW

        // חיבור רשימות הנתונים לטבלאות
        tblInstructions.setItems(rows);
        tblHistory.setItems(historyRows); // NEW: חשוב לחיבור הטבלה התחתונה

        // ניקוי ראשוני
        rows.clear();
        historyRows.clear(); // NEW
        tblInstructions.setDisable(true);
        lblBetweenTables.setText("");

        // NEW: מאזין לבחירה בטבלה העליונה – כל שינוי בחירה מנקה את הטבלה התחתונה
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

    public void bindProgram(Sprogram program) { bindProgram(program, false); }

    public void bindProgram(Sprogram program, boolean showNow) {
        this.program = program;

        // ניקוי טבלאות בעת הצמדה של תוכנית חדשה  // NEW
        historyRows.clear(); // NEW
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

    @FXML
    private void onProgramSelection() {
        if (program == null) return;
        rows.setAll(program.getInstructions());
        tblInstructions.setDisable(false);
        if (!rows.isEmpty()) tblInstructions.getSelectionModel().select(0);

        historyRows.clear(); // NEW: גם כאן ננקה את הטבלה התחתונה כשמראים/מרעננים את העליונה
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

    // לחיצה על Highlight – בונה שרשרת הורים ושופך לטבלה התחתונה
    private void onHighlightClicked() {
        Sinstruction sel = tblInstructions.getSelectionModel().getSelectedItem();
        if (sel == null) {
            historyRows.clear();
            return;
        }
        historyRows.setAll(buildParentChain(sel));
        if (!historyRows.isEmpty()) tblHistory.getSelectionModel().select(0);
    }

    /** בונה רשימת הורים מהבחירה עד השורש (הנבחר תחילה, אח״כ ההורה וכו׳) */
    private List<Sinstruction> buildParentChain(Sinstruction leaf) {
        List<Sinstruction> chain = new ArrayList<>();
        Sinstruction cur = leaf;
        while (cur != null) {
            chain.add(cur);
            cur = cur.getParentInstruction();
        }
        return chain;
    }

    // מציג ספירת פקודות בסיסיות/סינתטיות בלייבל שבין הטבלאות
    private void updateCountsLabel() {
        if (program == null) {
            lblBetweenTables.setText("");
            return;
        }
        int basic = program.getBasicInstructionNumber();
        int syn   = program.getSynteticInstructionNumber();
        lblBetweenTables.setText(
                "The number of basic instructions in the program is: " + basic + " | The number of synthetic instructions in the program is: " + syn
        );
    }

}
