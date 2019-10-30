package Messenger.Client;
import Messenger.Client.Controller.Controller;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Messenger extends Application {
    public static Scene scene;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Сетевой чат");
        stage.getIcons().add(new Image("Messenger/Client/stage_icon.png"));
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("scene.fxml"));
        Parent root = loader.load();

        scene = new Scene(root);

        Controller controller = loader.getController();
        stage.setOnHidden(e -> controller.shutdown());
        stage.setScene(scene);
        stage.setTitle("Messenger");
        stage.setX(900);
        stage.setY(400);
        stage.show();
    }
}