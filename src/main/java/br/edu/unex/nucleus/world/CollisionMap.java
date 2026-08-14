package br.edu.unex.nucleus.world;

import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Colisão do mundo.
 *
 * Para mapas Tiled que possuem uma object layer chamada "Colisão", os
 * retângulos/polígonos desenhados no próprio Tiled são usados diretamente.
 * Isso permite desenhar a colisão no editor e testar no jogo sem criar uma
 * imagem de máscara separada.
 *
 * Terrak, por exemplo, pode usar o arquivo terrak_com_colisao.tmx enviado
 * pelo usuário. As coordenadas dos objetos são convertidas do sistema do
 * Tiled para o sistema normalizado do mundo, usando a origem real dos chunks.
 */
public class CollisionMap {

    private final String mapId;
    private final List<Shape> blockedAreas = new ArrayList<>();

    private Image collisionImage;
    private PixelReader pixelReader;
    private boolean maskMode;
    private boolean tiledObjectMode;

    public CollisionMap() {
        this("");
    }

    public CollisionMap(boolean enabled) {
        this(enabled ? "terrak" : "");
    }

    public CollisionMap(String mapId) {
        this(mapId, null, null);
    }

    public CollisionMap(String mapId, TiledMap tiledMap, String tiledMapResource) {
        this.mapId = mapId == null ? "" : mapId.toLowerCase();

        if (tiledMap != null && tiledMapResource != null) {
            loadTiledCollisionObjects(tiledMap, tiledMapResource);
        }

        // Os três mapas atuais usam a camada "Colisão" do Tiled.
        if (blockedAreas.isEmpty()) {
            loadCollisionImage();

        } else {
            tiledObjectMode = true;
            maskMode = false;
        }
    }

    public boolean canMoveTo(double x, double y, double width, double height) {
        if (width <= 0 || height <= 0) {
            return false;
        }

        Rectangle2D player = new Rectangle2D(x, y, width, height);

        for (Shape blocked : blockedAreas) {
            if (intersects(blocked, player)) {
                return false;
            }
        }

        // Quando a colisão veio do Tiled, os objetos já representam o mapa
        // inteiro; não precisamos consultar uma máscara adicional.
        if (tiledObjectMode) {
            return true;
        }

        return !touchesBlockedTerrain(player);
    }

    public String getMapId() {
        return mapId;
    }

    public boolean usesTiledObjects() {
        return tiledObjectMode;
    }

    private void loadTiledCollisionObjects(TiledMap map, String resourcePath) {
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                System.err.println("[CollisionMap] TMX não encontrado: " + resourcePath);
                return;
            }

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(stream);

            NodeList groups = doc.getElementsByTagName("objectgroup");
            double originX = map.getMinTileX() * map.getTileSet().tileWidth();
            double originY = map.getMinTileY() * map.getTileSet().tileHeight();

            for (int i = 0; i < groups.getLength(); i++) {
                Element group = (Element) groups.item(i);
                if (!"Colisão".equalsIgnoreCase(group.getAttribute("name"))) {
                    continue;
                }

                NodeList objects = group.getElementsByTagName("object");
                for (int j = 0; j < objects.getLength(); j++) {
                    Element object = (Element) objects.item(j);
                    addCollisionObject(object, originX, originY);
                }

                if (objects.getLength() > 0) {
                    System.out.println("[CollisionMap] " + mapId + ": "
                            + objects.getLength() + " objetos de colisão carregados do Tiled.");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Falha ao carregar a camada Colisão do Tiled: " + resourcePath, e);
        }
    }

    private void addCollisionObject(Element object, double originX, double originY) {
        double x = parseDouble(object.getAttribute("x")) - originX;
        double y = parseDouble(object.getAttribute("y")) - originY;
        double rotation = parseDouble(object.getAttribute("rotation"));

        NodeList polygonNodes = object.getElementsByTagName("polygon");
        if (polygonNodes.getLength() > 0) {
            Element polygonElement = (Element) polygonNodes.item(0);
            String raw = polygonElement.getAttribute("points");
            String[] points = raw.trim().split(" ");
            Polygon polygon = new Polygon();

            double angle = Math.toRadians(rotation);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            for (String point : points) {
                String[] xy = point.split(",");
                if (xy.length != 2) continue;

                double px = parseDouble(xy[0]);
                double py = parseDouble(xy[1]);

                // Rotação de objetos do Tiled acontece ao redor da origem
                // (x,y) do objeto. Depois deslocamos para a origem do mundo.
                double rx = px * cos - py * sin;
                double ry = px * sin + py * cos;

                polygon.getPoints().addAll(x + rx, y + ry);
            }

            if (polygon.getPoints().size() >= 6) {
                blockedAreas.add(polygon);
            }
            return;
        }

        String widthAttr = object.getAttribute("width");
        String heightAttr = object.getAttribute("height");
        if (!widthAttr.isBlank() && !heightAttr.isBlank()) {
            double width = parseDouble(widthAttr);
            double height = parseDouble(heightAttr);

            Rectangle rectangle = new Rectangle(x, y, width, height);
            if (Math.abs(rotation) > 0.0001) {
                rectangle.setRotate(rotation);
            }
            blockedAreas.add(rectangle);
        }
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0;
        return Double.parseDouble(value);
    }

    private void loadCollisionImage() {
        String resource;

        switch (mapId) {
            case "terrak", "ignar", "nerion" -> {
                resource = "/backgrounds/" + mapId + "_collision.png";
                maskMode = true;
            }
            case "zephyron" -> {
                resource = "/backgrounds/zephyron.jpeg";
                maskMode = false;
            }
            case "floresta" -> {
                resource = "/backgrounds/floresta_sombria.png";
                maskMode = false;
            }
            default -> {
                maskMode = false;
                return;
            }
        }

        InputStream stream = getClass().getResourceAsStream(resource);
        if (stream == null) {
            System.err.println("[CollisionMap] Imagem de colisão não encontrada: " + resource);
            return;
        }

        collisionImage = new Image(stream);
        pixelReader = collisionImage.getPixelReader();
    }

    private boolean touchesBlockedTerrain(Rectangle2D r) {
        if (pixelReader == null || collisionImage == null) return false;

        double insetX = Math.min(3, r.getWidth() * 0.05);
        double insetY = Math.min(3, r.getHeight() * 0.05);
        double left = r.getMinX() + insetX;
        double right = r.getMaxX() - insetX;
        double top = r.getMinY() + insetY;
        double bottom = r.getMaxY() - insetY;

        final double step = maskMode ? 8.0 : 1.0;
        for (double y = top; y <= bottom; y += step) {
            for (double x = left; x <= right; x += step) {
                if (isBlockedPixel(x, y)) return true;
            }
        }

        return isBlockedPixel(right, top)
                || isBlockedPixel(left, bottom)
                || isBlockedPixel(right, bottom);
    }

    private boolean isBlockedPixel(double worldX, double worldY) {
        if (worldX < 0 || worldY < 0
                || worldX >= collisionImage.getWidth()
                || worldY >= collisionImage.getHeight()) {
            return true;
        }

        Color color = pixelReader.getColor(
                (int) Math.floor(worldX),
                (int) Math.floor(worldY));

        if (maskMode) {
            return color.getBrightness() < 0.50;
        }

        double r = color.getRed() * 255.0;
        double g = color.getGreen() * 255.0;
        double b = color.getBlue() * 255.0;

        return switch (mapId) {
            case "terrak" -> isTerrakBlockedPixel(r, g, b);
            case "ignar" -> isIgnarBlockedPixel(r, g, b);
            case "nerion" -> isNerionBlockedPixel(r, g, b);
            default -> false;
        };
    }

    private boolean isTerrakBlockedPixel(double r, double g, double b) {
        return b > 70 && b > r * 1.20 && b > g * 1.02;
    }

    private boolean isIgnarBlockedPixel(double r, double g, double b) {
        return r > 70 && r > g * 1.45 && r > b * 1.90;
    }

    private boolean isNerionBlockedPixel(double r, double g, double b) {
        return b > 45 && b > r * 1.35 && b > g * 1.18;
    }


    private boolean intersects(Shape blocked, Rectangle2D bounds) {
        Rectangle player = new Rectangle(
                bounds.getMinX(), bounds.getMinY(),
                bounds.getWidth(), bounds.getHeight());

        Shape intersection = Shape.intersect(player, blocked);
        Bounds result = intersection.getBoundsInLocal();
        return result.getWidth() > 0.01 && result.getHeight() > 0.01;
    }

}
