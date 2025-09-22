package semulator.display;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class Main extends Application {
    @Override public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(
                Main.class.getResource("/semulator/userInterface/mainBar/MyFxmi.fxml")));

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("S-emulator");
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
