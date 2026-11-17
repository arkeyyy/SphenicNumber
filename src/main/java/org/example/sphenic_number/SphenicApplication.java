package org.example.sphenic_number;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SphenicApplication extends Application {
    private SphenicController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(SphenicApplication.class.getResource("sphenic-view.fxml"));
        Scene scene = new Scene(loader.load(), 960, 720);
        controller = loader.getController();
        stage.setTitle("Sphenic Number Explorer");
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> controller.shutdown());
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }
}
