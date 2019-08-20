package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;

import main.mainWindow.MainWindowController;
import org.fxmisc.cssfx.CSSFX;

public class AppMain extends Application {
    public static void main(String[] args) {

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        CSSFX.start();

        FXMLLoader loader = new FXMLLoader();

        // load main fxml
        URL mainFXML = getClass().getResource("/main/mainWindow/mainWindow.fxml");
        loader.setLocation(mainFXML);
        BorderPane root = loader.load();

        // wire up controller
        MainWindowController mainWindowController = loader.getController();
        MainEngine engine = new MainEngine(mainWindowController);
        mainWindowController.setPrimaryStage(primaryStage);
        mainWindowController.setEngine(engine);

        // set stage
        primaryStage.setTitle("My amazing git");
        Scene scene = new Scene(root, 1050, 600);
        primaryStage.setScene(scene);
        primaryStage.show();


    }
}
