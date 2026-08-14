package br.edu.unex.nucleus.rendering;

import br.edu.unex.nucleus.ai.EnemyState;
import br.edu.unex.nucleus.entity.Enemy;
import br.edu.unex.nucleus.entity.HealthPotion;
import br.edu.unex.nucleus.entity.Player;
import br.edu.unex.nucleus.entity.Portal;
import br.edu.unex.nucleus.entity.Projectile;
import br.edu.unex.nucleus.world.Island;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Renderer {

    private final Canvas canvas;
    private final GraphicsContext graphicsContext;

    public Renderer(Canvas canvas) {
        this.canvas = canvas;
        this.graphicsContext = canvas.getGraphicsContext2D();
        this.graphicsContext.setImageSmoothing(false);
    }

    public double calculateCameraX(double playerX, double worldWidth) {
        return clamp(
                playerX + 16 - canvas.getWidth() / 2.0,
                0,
                Math.max(0, worldWidth - canvas.getWidth())
        );
    }

    public double calculateCameraY(double playerY, double worldHeight) {
        return clamp(
                playerY + 16 - canvas.getHeight() / 2.0,
                0,
                Math.max(0, worldHeight - canvas.getHeight())
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public void clear() {
        graphicsContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void clear(Color backgroundColor) {
        clear();
        graphicsContext.setFill(backgroundColor);
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphicsContext.setFill(Color.BLACK);
    }

    public void drawBackground(Island island, double cameraX, double cameraY) {
        clear();
        graphicsContext.setImageSmoothing(false);

        if (island.getTiledMap() != null) {
            drawTiledMap(island.getTiledMap(), cameraX, cameraY);
            return;
        }

        if (island.getBackgroundImage() != null) {
            graphicsContext.drawImage(island.getBackgroundImage(), -cameraX, -cameraY);
        } else {
            clear(island.getBackgroundColor());
        }
    }

    private void drawTiledMap(br.edu.unex.nucleus.world.TiledMap map, double cameraX, double cameraY) {
        var ts = map.getTileSet();
        int tw = ts.tileWidth();
        int th = ts.tileHeight();
        // Os novos mapas do Tiled são infinitos e podem possuir chunks com coordenadas negativas.
        // cameraX/cameraY usam coordenadas normalizadas do mundo, enquanto
        // os mapas infinitos do Tiled podem começar em coordenadas negativas.
        // Portanto, convertemos a janela visível de volta para a coordenada
        // original do tile somando a origem minTileX/minTileY.
        int firstX = Math.max(
                map.getMinTileX(),
                map.getMinTileX() + (int) Math.floor(cameraX / tw) - 1
        );
        int firstY = Math.max(
                map.getMinTileY(),
                map.getMinTileY() + (int) Math.floor(cameraY / th) - 1
        );
        int lastX = Math.min(
                map.getMaxTileX(),
                map.getMinTileX() + (int) Math.ceil((cameraX + canvas.getWidth()) / tw)
        );
        int lastY = Math.min(
                map.getMaxTileY(),
                map.getMinTileY() + (int) Math.ceil((cameraY + canvas.getHeight()) / th)
        );

        for (int y = firstY; y <= lastY; y++) {
            for (int x = firstX; x <= lastX; x++) {
                int gid = map.getGid(x, y);
                if (gid <= 0) continue;
                int local = gid - 1;
                int sx = (local % ts.columns()) * tw;
                int sy = (local / ts.columns()) * th;
                graphicsContext.drawImage(ts.image(), sx, sy, tw, th,
                        (x - map.getMinTileX()) * tw - cameraX,
                        (y - map.getMinTileY()) * th - cameraY,
                        tw, th);
            }
        }
    }

    public void drawPlayer(Player player, double cameraX, double cameraY) {
        double drawX = player.getX() - cameraX;
        double drawY = player.getY() - cameraY;

        player.getAnimator().draw(
                graphicsContext,
                drawX,
                drawY,
                player.getWidth(),
                player.getHeight()
        );
    }

    public void drawEnemy(Enemy enemy, double cameraX, double cameraY) {
        if (enemy.isDead()) {
            return;
        }
        double drawX = enemy.getX() - enemy.getWidth() / 2 - cameraX;
        double drawY = enemy.getY() - enemy.getHeight() / 2 - cameraY;

        if (enemy.isBoss()) {
            graphicsContext.setFill(Color.rgb(70, 180, 50, 0.22));
            graphicsContext.fillOval(drawX - 18, drawY - 18, enemy.getWidth() + 36, enemy.getHeight() + 36);
            graphicsContext.setStroke(Color.LIMEGREEN);
            graphicsContext.setLineWidth(3);
            graphicsContext.strokeOval(drawX - 18, drawY - 18, enemy.getWidth() + 36, enemy.getHeight() + 36);
            graphicsContext.setLineWidth(1);
        }

        enemy.getAnimator().draw(
                graphicsContext,
                drawX,
                drawY,
                enemy.getWidth(),
                enemy.getHeight()
        );

        // Pequeno indicador de estado durante desenvolvimento.
        graphicsContext.setStroke(colorForState(enemy.getState()));
        graphicsContext.setLineWidth(1);
        graphicsContext.strokeRect(
                drawX,
                drawY,
                enemy.getWidth(),
                enemy.getHeight()
        );
        graphicsContext.setStroke(Color.BLACK);
    }

    public void drawEnemyDebugShapes(Enemy enemy, double cameraX, double cameraY) {
        // Mantido para desenvolvimento; desenha as áreas de IA na posição da câmera.
        graphicsContext.setStroke(Color.rgb(80, 80, 200, 0.35));
        graphicsContext.strokeOval(
                enemy.getX() - enemy.getHearingRadius() - cameraX,
                enemy.getY() - enemy.getHearingRadius() - cameraY,
                enemy.getHearingRadius() * 2,
                enemy.getHearingRadius() * 2
        );

        graphicsContext.setFill(Color.rgb(255, 220, 0, 0.12));
        double startAngle = -(enemy.getDirectionAngle() + enemy.getVisionHalfAngle());
        double arcExtent = enemy.getVisionHalfAngle() * 2;

        graphicsContext.fillArc(
                enemy.getX() - enemy.getVisionRange() - cameraX,
                enemy.getY() - enemy.getVisionRange() - cameraY,
                enemy.getVisionRange() * 2,
                enemy.getVisionRange() * 2,
                startAngle,
                arcExtent,
                javafx.scene.shape.ArcType.ROUND
        );

        graphicsContext.setFill(Color.BLACK);
        graphicsContext.setStroke(Color.BLACK);
    }

    private Color colorForState(EnemyState state) {
        return switch (state) {
            case PATROL -> Color.GREEN;
            case ALERT -> Color.ORANGE;
            case CHASE -> Color.RED;
            case ATTACK -> Color.DARKRED;
            case RETURN -> Color.GRAY;
        };
    }

    public void drawPortal(Portal portal, double cameraX, double cameraY) {
        double drawX = portal.getX() - cameraX;
        double drawY = portal.getY() - cameraY;

        graphicsContext.setFill(Color.rgb(180, 100, 220, 0.7));
        graphicsContext.fillOval(
                drawX,
                drawY,
                portal.getWidth(),
                portal.getHeight()
        );

        graphicsContext.setStroke(Color.rgb(120, 40, 160));
        graphicsContext.setLineWidth(2);
        graphicsContext.strokeOval(
                drawX,
                drawY,
                portal.getWidth(),
                portal.getHeight()
        );

        graphicsContext.setFill(Color.BLACK);
        graphicsContext.setStroke(Color.BLACK);
        graphicsContext.setLineWidth(1);
    }

    public void drawHealthPotion(HealthPotion potion, double cameraX, double cameraY) {
        if (potion.isCollected()) return;
        double x = potion.getX() - cameraX;
        double y = potion.getY() - cameraY;
        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.35));
        graphicsContext.fillOval(x - 11, y - 7, 22, 14);
        graphicsContext.setFill(Color.CRIMSON);
        graphicsContext.fillRoundRect(x - 7, y - 12, 14, 20, 5, 5);
        graphicsContext.setFill(Color.LIGHTPINK);
        graphicsContext.fillRect(x - 5, y - 9, 4, 8);
        graphicsContext.setFill(Color.LIGHTGRAY);
        graphicsContext.fillRect(x - 5, y - 17, 10, 6);
        graphicsContext.setStroke(Color.WHITE);
        graphicsContext.strokeRoundRect(x - 7, y - 12, 14, 20, 5, 5);
        graphicsContext.setStroke(Color.BLACK);
    }

    public void drawHealthPotionHud(Player player) {
        double x = 255;
        double y = canvas.getHeight() - 40;
        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.75));
        graphicsContext.fillRoundRect(x - 8, y - 5, 150, 28, 8, 8);
        graphicsContext.setFill(Color.CRIMSON);
        graphicsContext.fillRoundRect(x, y, 18, 18, 4, 4);
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillText("Poções: " + player.getHealthPotions() + "  [H]", x + 26, y + 14);
    }

    public void drawProjectile(Projectile projectile, double cameraX, double cameraY) {
        graphicsContext.setFill(Color.rgb(255, 80, 0));
        graphicsContext.fillOval(
                projectile.getX() - cameraX,
                projectile.getY() - cameraY,
                projectile.getWidth(),
                projectile.getHeight()
        );
        graphicsContext.setFill(Color.BLACK);
    }

    public void drawPlayerHealth(Player player) {
        double x = 18;
        double y = canvas.getHeight() - 38;
        double width = 220;
        double height = 18;

        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.75));
        graphicsContext.fillRoundRect(x - 4, y - 4, width + 8, height + 8, 8, 8);
        graphicsContext.setFill(Color.DARKRED);
        graphicsContext.fillRect(x, y, width, height);
        graphicsContext.setFill(Color.LIMEGREEN);
        graphicsContext.fillRect(x, y, width * (player.getHealth() / player.getMaxHealth()), height);
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillText(
                String.format("Vida: %.0f / %.0f", player.getHealth(), player.getMaxHealth()),
                x + 8, y + 14
        );
    }

    public void drawBossBars(Island island) {
        for (Enemy enemy : island.getEnemies()) {
            if (!enemy.isBoss() || enemy.isDead()) {
                continue;
            }

            double width = 420;
            double height = 22;
            double x = (canvas.getWidth() - width) / 2;
            double y = 28;

            graphicsContext.setFill(Color.rgb(0, 0, 0, 0.85));
            graphicsContext.fillRoundRect(x - 6, y - 6, width + 12, height + 30, 10, 10);
            graphicsContext.setFill(Color.DARKRED);
            graphicsContext.fillRect(x, y, width, height);
            graphicsContext.setFill(Color.FORESTGREEN);
            graphicsContext.fillRect(x, y, width * (enemy.getHealth() / enemy.getMaxHealth()), height);
            graphicsContext.setFill(Color.WHITE);
            String bossName = enemy instanceof br.edu.unex.nucleus.world.ElementBoss eb
                    ? eb.getBossName() : "TERRAK — SENHOR DA TERRA";
            graphicsContext.fillText(bossName, x + 12, y + 16);
            return;
        }
    }

    public void drawVictoryOverlay() {
        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.68));
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphicsContext.setFill(Color.LIMEGREEN);
        graphicsContext.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 34));
        graphicsContext.fillText("CAMPANHA CONCLUÍDA!", 245, 275);
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(javafx.scene.text.Font.font("System", 18));
        graphicsContext.fillText("Os três Senhores Elementais foram derrotados.", 230, 312);
    }

    public void drawGameOverOverlay() {
        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.72));
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphicsContext.setFill(Color.CRIMSON);
        graphicsContext.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 36));
        graphicsContext.fillText("VOCÊ FOI DERROTADO", 220, 275);
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(javafx.scene.text.Font.font("System", 18));
        graphicsContext.fillText("Pressione R para voltar à entrada do Vale.", 260, 312);
    }

    public void drawIslandDebug(Island island) {
        // Mantém o nome do mapa, mas evita poluir o mapa com informações técnicas.
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillText(island.getDisplayName(), 12, 22);
        graphicsContext.setFill(Color.BLACK);
    }

    public void drawDebug(Player player, double deltaTime, double cameraX, double cameraY) {
        // Informações técnicas discretas no canto superior esquerdo.
        graphicsContext.setFill(Color.rgb(255, 255, 255, 0.85));
        graphicsContext.fillText(
                String.format("X=%.0f Y=%.0f | FPS=%.0f",
                        player.getX(), player.getY(),
                        deltaTime > 0 ? 1.0 / deltaTime : 0),
                12,
                42
        );
        graphicsContext.setFill(Color.BLACK);
    }
}
