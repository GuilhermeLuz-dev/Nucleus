package br.edu.unex.nucleus.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

import java.util.HashSet;
import java.util.Set;

public class InputManager {

    private final Set<KeyCode> keysPressed = new HashSet<>();

    public InputManager(Scene scene) {

        scene.setOnKeyPressed(event -> {
            keysPressed.add(event.getCode());
        });

        scene.setOnKeyReleased(event -> {
            keysPressed.remove(event.getCode());
        });
    }

    public boolean isPressed(KeyCode key) {
        return keysPressed.contains(key);
    }
}