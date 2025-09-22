package semulator.userInterface.LeftBar;


import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.fxml.FXML;
import semulator.logic.program.Sprogram;


public class LeftBarController {

    Sprogram program;
    @FXML private HBox toolbar;

    // Top toolbar
    @FXML private Button btnProgramSelector;
    @FXML private Button btnCollapse;
    @FXML private Button btnExpand;
    @FXML private Button btnCurrentMaxDegree;
    @FXML private Button btnHighlightSelection;

    // Upper table (instructions)
    @FXML private TableView<?> tblInstructions;
    @FXML private TableColumn<?, Number> colIdx;
    @FXML private TableColumn<?, String>  colBS;
    @FXML private TableColumn<?, Number> colCycles;
    @FXML private TableColumn<?, String>  colInstruction;

    // Between tables
    @FXML private Label lblBetweenTables;

    // Lower table (history/chain)
    @FXML private TableView<?> tblHistory;
    @FXML private TableColumn<?, Number> colHIdx;
    @FXML private TableColumn<?, String>  colHBS;
    @FXML private TableColumn<?, Number> colHCycles;
    @FXML private TableColumn<?, String>  colHInstruction;


    @FXML
    private void initialize() {

        for (var node : toolbar.getChildren()) {
            if (node instanceof Button) {
                Button b = (Button) node;
                b.setFocusTraversable(false);
                b.setOnAction(e -> flash(b, 1));
            }
        }
    }


        private void flash (Button b ,double seconds) {
            // הבזק כחול זמני
            if (!b.getStyleClass().contains("flash")) {
                b.getStyleClass().add("flash");
            }
            PauseTransition pt = new PauseTransition(Duration.seconds(seconds));
            pt.setOnFinished(ev -> b.getStyleClass().remove("flash"));
            pt.play();
        }

    public void bindProgram(Sprogram program) {
        this.program = program;
       }

}




