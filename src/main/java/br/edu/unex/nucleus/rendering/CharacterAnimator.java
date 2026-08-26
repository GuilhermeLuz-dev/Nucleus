package br.edu.unex.nucleus.rendering;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Controla spritesheets 64x64 com 4 direções (uma direção por linha).
 * Linha 0 = frente, 1 = costas, 2 = esquerda, 3 = direita.
 */
public class CharacterAnimator {

    public enum Direction {
        FRONT, BACK, LEFT, RIGHT
    }

    public enum Action {
        IDLE, WALK, ATTACK, HURT, DEATH
    }

    private final Map<Action, Image> sheets = new EnumMap<>(Action.class);
    private final Map<Action, Integer> frameCounts = new EnumMap<>(Action.class);

    private Direction direction = Direction.FRONT;
    private Action action = Action.IDLE;

    private int frame;
    private double timer;
    private boolean finished;

    public CharacterAnimator(String basePath) {
        this(basePath, 4, 6, 12, 4, 11);
    }

    /**
     * Permite usar spritesheets com quantidades de quadros diferentes.
     * Cada quadro continua sendo 64x64 e as quatro direções ficam em linhas.
     */
    public CharacterAnimator(String basePath,
                             int idleFrames, int walkFrames, int attackFrames,
                             int hurtFrames, int deathFrames) {
        load(Action.IDLE, basePath + "/idle.png", idleFrames);
        load(Action.WALK, basePath + "/walk.png", walkFrames);
        load(Action.ATTACK, basePath + "/attack.png", attackFrames);
        load(Action.HURT, basePath + "/hurt.png", hurtFrames);
        load(Action.DEATH, basePath + "/death.png", deathFrames);
    }

    private void load(Action action, String path, int frameCount) {
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            sheets.put(action, new Image(stream));
            frameCounts.put(action, frameCount);
        }
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setAction(Action action) {
        if (this.action != action) {
            this.action = action;
            this.frame = 0;
            this.timer = 0;
            this.finished = false;
        }
    }

    public void update(double deltaTime) {
        Image sheet = sheets.get(action);
        Integer count = frameCounts.get(action);

        if (sheet == null || count == null || count <= 1) {
            return;
        }

        double frameDuration = switch (action) {
            case IDLE -> 0.16;
            case WALK -> 0.10;
            case ATTACK -> 0.055;
            case HURT -> 0.08;
            case DEATH -> 0.09;
        };

        timer += deltaTime;

        while (timer >= frameDuration) {
            timer -= frameDuration;

            if (action == Action.DEATH) {
                if (frame < count - 1) {
                    frame++;
                } else {
                    finished = true;
                }
            } else {
                // Ataque e demais animações não terminais podem repetir.
                frame = (frame + 1) % count;
            }
        }
    }

    public void draw(GraphicsContext gc, double x, double y, double width, double height) {
        Image sheet = sheets.get(action);

        if (sheet == null) {
            return;
        }

        int row = switch (direction) {
            case FRONT -> 0;
            case BACK -> 1;
            case LEFT -> 2;
            case RIGHT -> 3;
        };

        int frameWidth = 64;
        int frameHeight = 64;

        gc.drawImage(
                sheet,
                frame * frameWidth,
                row * frameHeight,
                frameWidth,
                frameHeight,
                x,
                y,
                width,
                height
        );
    }

    public boolean hasAnimation(Action action) {
        return sheets.containsKey(action);
    }

    public boolean isFinished() {
        return finished;
    }

    public Action getAction() {
        return action;
    }

    public Direction getDirection() {
        return direction;
    }
}
