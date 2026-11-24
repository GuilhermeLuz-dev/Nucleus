package br.edu.unex.nucleus.entity;

import javafx.scene.image.Image;
import java.io.InputStream;

/** NPC usado somente no encerramento da campanha. */
public class FinalBrother {
    private final String name;
    private final double x;
    private final double y;
    private final double width = 72;
    private final double height = 84;
    private final Image image;

    public FinalBrother(String name, double x, double y, String imagePath) {
        this.name = name;
        this.x = x;
        this.y = y;

        InputStream stream = getClass().getResourceAsStream(imagePath);
        if (stream == null) throw new IllegalArgumentException(
                "Sprite do irmão não encontrado: " + imagePath);
        this.image = new Image(stream);
    }

    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public Image getImage() { return image; }
}
