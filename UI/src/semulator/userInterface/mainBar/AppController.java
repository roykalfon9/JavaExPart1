package semulator.userInterface.mainBar;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.StageStyle;
import semulator.display.RunRecord;
import semulator.logic.program.Sprogram;
import semulator.userInterface.LeftBar.LeftBarController;
import semulator.userInterface.rightBar.RightBarController;
import semulator.userInterface.topBar.TopBarController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AppController {

    private Sprogram baseProgram;
    private Sprogram currentProgram;
    private int maxDegree = 0;

    // --- היסטוריה + נתיב אחרון ---
    private final List<RunRecord> history = new ArrayList<>();
    private int runCounter = 0;
    private Path lastSourcePath = null;

    @FXML private TopBarController   topBarController;
    @FXML private LeftBarController  leftBarController;
    @FXML private RightBarController rightBarController;

    @FXML
    private void initialize() {
        if (topBarController  != null) topBarController.setAppController(this);
        if (leftBarController != null) leftBarController.setAppController(this);
        if (rightBarController!= null) rightBarController.setAppController(this);
    }

    /** יקרא מטופ־בר לאחר טעינת XML מוצלחת */
    public void onProgramLoaded(Sprogram program, String sourcePath) {
        this.baseProgram = program;
        this.currentProgram = program;
        this.maxDegree = (program != null) ? program.calculateMaxDegree() : 0;

        this.lastSourcePath = (sourcePath != null && !sourcePath.isBlank())
                ? Paths.get(sourcePath)
                : null;

        if (leftBarController  != null) { leftBarController.resetExpandLevel(); leftBarController.bindProgram(program); }
        if (rightBarController != null) { rightBarController.bindProgram(program); }
    }

    public void requestExpandTo(int level, LeftBarController origin) {
        if (baseProgram == null) { showError("No program loaded","Please load a program first."); return; }
        if (level > maxDegree) {
            origin.setCurrExpandLevel(level-1);
            showError("Cannot expand","Current expand level equals/exceeds program max degree ("+maxDegree+")."); return;
        }
        if (level < 0) {
            origin.setCurrExpandLevel(level+1);
            showError("Cannot collapse","Min degree is 0."); return;
        }

        try {
            Sprogram expanded;
            if (level == 0)
            {
                 expanded = baseProgram;
            }
            else
            {
                 expanded = baseProgram.expand(level);
            }

            this.currentProgram = expanded;

            if (leftBarController  != null) leftBarController.bindProgram(expanded, true);
            if (rightBarController != null) rightBarController.bindProgram(expanded);

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

    public int getMaxDegree() { return maxDegree; }
    public Sprogram getCurrentProgram() { return currentProgram; }
    public Path getLastSourcePath() { return lastSourcePath; }
    public int getCurrentExpandLevel() {
        return (leftBarController != null) ? leftBarController.getCurrExpandLevel() : 0;
    }

    // --- היסטוריה: הוספה + הפצה גם לימין ---
    public void addRunRecord(RunRecord rec) {
        history.add(rec);
        if (rightBarController != null) rightBarController.addRunRecord(rec);
    }

    public int nextRunNumber() { runCounter += 1; return runCounter; }
    public RunRecord getLastRunRecord() { return history.isEmpty() ? null : history.get(history.size()-1); }

    // --- RERUN מ־RightBar ---
    public void rerunRecord(RunRecord rec) {
        if (rec == null || rightBarController == null) return;

        boolean needReload = (rec.getPath() != null && (lastSourcePath == null || !rec.getPath().equals(lastSourcePath)));

        if (needReload && topBarController != null) {
            // צריך שתממש ב-TopBarController:
            // public void loadProgramFromPath(String path, Runnable onSuccess)
            topBarController.loadProgramFromPath(rec.getPath().toString(), () -> {
                rightBarController.fillInputs(rec.getInputs());
                rightBarController.triggerStartWithoutHistory();
            });
        } else {
            rightBarController.fillInputs(rec.getInputs());
            rightBarController.triggerStartWithoutHistory();
        }
    }

    /** נדרש לדיבאג כדי להדגיש שורה בלפט־בר */ // ADDED
    public LeftBarController getLeftBarController() {
        return leftBarController;
    }
}
