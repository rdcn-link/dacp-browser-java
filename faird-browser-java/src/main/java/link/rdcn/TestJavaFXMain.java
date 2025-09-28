package link.rdcn;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;


public class TestJavaFXMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("main.fxml")));

        primaryStage.setTitle("test");
        Scene scene = new Scene(root);

        scene.getStylesheets().add(getClass().getResource("/style_fx.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        System.out.println("1111111");

//        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.show();

    }
    public static void main(String[] args) {
        launch(args);
    }
}
