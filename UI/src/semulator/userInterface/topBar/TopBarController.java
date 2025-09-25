package semulator.userInterface.topBar;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import semulator.logic.program.Sprogram;
import semulator.logic.xml.xmlreader.XMLParser;
import semulator.userInterface.mainBar.AppController;

import java.io.File;

public class TopBarController {

    @FXML private TextField pathField;

    private AppController app;
    public void setAppController(AppController app) { this.app = app; }

    @FXML
    private void onBrowseFile(ActionEvent e) {
        Window owner = ((Node) e.getSource()).getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose XML file");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        // פתיחה אחרונה (אם קיימת)
        if (pathField.getText() != null && !pathField.getText().isBlank()) {
            File hint = new File(pathField.getText()).getParentFile();
            if (hint != null && hint.isDirectory()) fc.setInitialDirectory(hint);
        }

        File f = fc.showOpenDialog(owner);
        if (f != null) {
            pathField.setText(f.getAbsolutePath());
            loadXmlIntoAppAsync(f.getAbsolutePath(), owner);
        }
    }

    private void loadXmlIntoAppAsync(String filePath, Window owner) {
        Stage progressStage = createProgressStage(owner);
        ProgressBar bar = (ProgressBar) ((VBox) progressStage.getScene().getRoot()).getChildren().get(0);
        Label label     = (Label)     ((VBox) progressStage.getScene().getRoot()).getChildren().get(1);

        Task<Sprogram> task = new Task<>() {
            @Override
            protected Sprogram call() throws Exception {
                updateProgress(0, 1);
                updateMessage("Validating path…");
                XMLParser.validateXmlFilePath(filePath);

                updateProgress(0.25, 1);
                updateMessage("Parsing XML…");
                XMLParser parser = new XMLParser();
                Sprogram program = parser.loadProgramFromXML(filePath);

                updateProgress(0.75, 1);
                updateMessage("Validating program…");
                if (program == null) {
                    throw new IllegalStateException("Loaded program is null.");
                }

                if (!program.validate()) {
                    throw new IllegalStateException("Program validation failed: a jump targets an undefined label.");
                }

                updateProgress(1, 1);
                updateMessage("Done");
                return program;
            }
        };

        bar.progressProperty().bind(task.progressProperty());
        label.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(ev -> {
            progressStage.close();
            Sprogram program = task.getValue();
            if (app != null) {
                Platform.runLater(() -> app.onProgramLoaded(program, filePath));
            } else {
                showError("App wiring error", "AppController is null – please ensure setAppController() is called.");
            }
        });

        task.setOnFailed(ev -> {
            progressStage.close();
            Throwable ex = task.getException();
            String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : String.valueOf(ex);
            showError("Failed to load program", msg);
        });

        progressStage.show();
        Thread t = new Thread(task, "xml-load-task");
        t.setDaemon(true);
        t.start();
    }

    private Stage createProgressStage(Window owner) {
        ProgressBar pb = new ProgressBar(0);
        pb.setPrefWidth(280);
        Label msg = new Label("Starting…");

        VBox box = new VBox(10, pb, msg);
        box.setStyle("-fx-padding: 14;");

        Stage s = new Stage();
        s.initOwner(owner);
        s.initModality(Modality.WINDOW_MODAL);
        s.setTitle("Loading");
        s.setResizable(false);
        s.setScene(new Scene(box));
        return s;
    }

    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initOwner(pathField.getScene().getWindow());
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(content);

        var dp = a.getDialogPane();

        // נתיב נכון לקובץ שלך:
        String dialogCss = getClass()
                .getResource("/semulator/userInterface/mainBar/dialogs-dark.css")
                .toExternalForm();
        dp.getStylesheets().add(dialogCss);
        dp.getStyleClass().add("app-dialog");

        // אופציונלי:ורשה גם את ה-CSS הראשי של הסצנה
        dp.getStylesheets().addAll(pathField.getScene().getStylesheets());

        a.showAndWait();
    }
}
