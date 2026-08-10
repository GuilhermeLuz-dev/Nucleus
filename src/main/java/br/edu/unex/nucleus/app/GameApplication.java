package br.edu.unex.nucleus.app;

import br.edu.unex.nucleus.core.GameEngine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GameApplication extends Application {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    @Override
    public void start(Stage stage) {

        Canvas canvas = new Canvas(WIDTH, HEIGHT);

        StackPane root = new StackPane(canvas);

        Scene scene = new Scene(root);

        GameEngine gameEngine = new GameEngine(canvas, scene);

        gameEngine.start();

        stage.setTitle("Nucleus");

        stage.setScene(scene);

        stage.setResizable(false);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}