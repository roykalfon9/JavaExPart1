package semulator.userInterface.mainBar;

import javafx.fxml.FXML;
import semulator.logic.program.Sprogram;
import semulator.userInterface.topBar.TopBarController;
import semulator.userInterface.LeftBar.LeftBarController;
import semulator.userInterface.rightBar.RightBarController;

public class AppController {

    @FXML private TopBarController topBarController;
    @FXML private LeftBarController leftBarController;
    @FXML private RightBarController rightBarController;

    @FXML
    private void initialize() {
        if (topBarController != null) {
            topBarController.setAppController(this);
        }
    }

    /** יקרא מטופ־בר לאחר טעינת XML מוצלחת */
    public void onProgramLoaded(Sprogram program, String sourcePath) {
        // בהמשך: מילוי טבלאות, סינכרון מצב וכו’
        if (leftBarController != null)  leftBarController.bindProgram(program);
        if (rightBarController != null) rightBarController.bindProgram(program);
    }
}
