package semulator.userInterface.mainBar;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.StageStyle;
import semulator.logic.program.Sprogram;
import semulator.userInterface.topBar.TopBarController;
import semulator.userInterface.LeftBar.LeftBarController;
import semulator.userInterface.rightBar.RightBarController;

public class AppController {

    private Sprogram baseProgram;
    private Sprogram currentProgram;
    private int maxDegree = 0;


    @FXML private TopBarController topBarController;
    @FXML private LeftBarController leftBarController;
    @FXML private RightBarController rightBarController;

    @FXML
    private void initialize() {
        if (topBarController != null) {
            topBarController.setAppController(this);
        }
        if (leftBarController != null) {
            leftBarController.setAppController(this);
        }
    }

    /** יקרא מטופ־בר לאחר טעינת XML מוצלחת */
    public void onProgramLoaded(Sprogram program, String sourcePath) {
        this.baseProgram = program;
        this.currentProgram = program;
        this.maxDegree = (program != null) ? program.calculateMaxDegree() : 0;
        leftBarController.resetExpandLevel();


        if (leftBarController != null)  leftBarController.bindProgram(program);
        if (rightBarController != null) rightBarController.bindProgram(program);
    }


    public void requestExpandTo(int level, LeftBarController origin) {
        if (baseProgram == null) { showError("No program loaded","Please load a program first."); return; }
        if (level > maxDegree) {
            origin.setCurrExpandLevel(level-1);
            showError("Cannot expand","Current expand level equals or exceeds program max degree ("+maxDegree+")."); return;
        }
        if (level < 0) {
            origin.setCurrExpandLevel(level+1);
            showError("Cannot expand","Current expand level equals or exceeds program min degree 0."); return;
        }


        try {
            Sprogram expanded;
            if (level == 0) {
                expanded = this.baseProgram;
            }
            else
            {
                expanded = baseProgram.expand(level);
                this.currentProgram = expanded;
            }

            leftBarController.bindProgram(expanded, true);
            rightBarController.bindProgram(expanded);

        } catch (Exception ex) {
            showError("Expand failed", ex.getMessage() != null ? ex.getMessage() : ex.toString());
            if (origin != null) origin.setCurrExpandLevel(Math.max(0, level - 1));
        }
    }

    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initStyle(StageStyle.UTILITY);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(content);
        a.getDialogPane().getStylesheets().add(
                getClass().getResource("/semulator/userInterface/mainBar/dialogs-dark.css").toExternalForm()
        );
        a.getDialogPane().getStyleClass().add("app-dialog");
        a.showAndWait();
    }

    public int getMaxDegree() {
        return maxDegree; // מחושב ב-onProgramLoaded מה-baseProgram בלבד
    }


}
