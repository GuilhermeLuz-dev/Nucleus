package br.edu.unex.nucleus.world;

import javafx.scene.image.Image;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Carrega mapas ortogonais exportados pelo Tiled.
 * Suporta mapas finitos e mapas infinitos (camadas com chunks CSV).
 */
public class TiledMap {
    public record MapObject(double x, double y, double width, double height, double rotation) {}
    public record TileSet(Image image, int tileWidth, int tileHeight, int columns) {}

    private final int width;
    private final int height;
    private final int minTileX;
    private final int minTileY;
    private final int maxTileX;
    private final int maxTileY;
    private final TileSet tileSet;
    private final Map<Long, Integer> gids;
    private final String currentResourcePath;

    public TiledMap(String resourcePath) {
        this.currentResourcePath = resourcePath;
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Mapa Tiled não encontrado: " + resourcePath);
            }

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(stream);
            Element map = doc.getDocumentElement();

            width = parseIntAttribute(map, "width", resourcePath);
            height = parseIntAttribute(map, "height", resourcePath);
            gids = new HashMap<>();

            int parsedMinX = 0;
            int parsedMinY = 0;
            int parsedMaxX = width - 1;
            int parsedMaxY = height - 1;

            Element tileset = (Element) map.getElementsByTagName("tileset").item(0);
            if (tileset == null) {
                throw new IllegalArgumentException("Mapa Tiled sem tileset: " + resourcePath);
            }

            Element imageElement = (Element) tileset.getElementsByTagName("image").item(0);
            if (imageElement == null) {
                throw new IllegalArgumentException("Tileset sem imagem: " + resourcePath);
            }

            String imagePath = imageElement.getAttribute("source");
            String base = resourcePath.substring(0, resourcePath.lastIndexOf('/') + 1);
            String normalized = base + imagePath;
            InputStream imageStream = getClass().getResourceAsStream(normalized);
            if (imageStream == null) {
                throw new IllegalArgumentException(
                        "Imagem do tileset não encontrada. O arquivo " + resourcePath
                                + " referencia " + normalized
                                + ". Coloque essa imagem em src/main/resources/backgrounds/.");
            }

            Image image;
            try (imageStream) {
                image = new Image(imageStream);
            }

            int tileWidth = parseIntAttribute(tileset, "tilewidth", resourcePath);
            int tileHeight = parseIntAttribute(tileset, "tileheight", resourcePath);
            int columns = parseIntAttribute(tileset, "columns", resourcePath);
            tileSet = new TileSet(image, tileWidth, tileHeight, columns);

            Element data = (Element) map.getElementsByTagName("data").item(0);
            if (data == null) {
                throw new IllegalArgumentException("Mapa Tiled sem camada de dados: " + resourcePath);
            }

            NodeList chunks = data.getElementsByTagName("chunk");
            if (chunks.getLength() > 0) {
                // Mapa infinito: cada chunk possui sua própria origem x/y.
                parsedMinX = Integer.MAX_VALUE;
                parsedMinY = Integer.MAX_VALUE;
                parsedMaxX = Integer.MIN_VALUE;
                parsedMaxY = Integer.MIN_VALUE;

                for (int i = 0; i < chunks.getLength(); i++) {
                    Element chunk = (Element) chunks.item(i);
                    int chunkX = parseIntAttribute(chunk, "x", resourcePath);
                    int chunkY = parseIntAttribute(chunk, "y", resourcePath);
                    int chunkWidth = parseIntAttribute(chunk, "width", resourcePath);
                    int chunkHeight = parseIntAttribute(chunk, "height", resourcePath);

                    String[] values = chunk.getTextContent()
                            .replaceAll("\\s+", "")
                            .trim()
                            .split(",");

                    for (int localY = 0; localY < chunkHeight; localY++) {
                        for (int localX = 0; localX < chunkWidth; localX++) {
                            int index = localY * chunkWidth + localX;
                            if (index >= values.length || values[index].isEmpty()) continue;

                            int gid = Integer.parseInt(values[index]);
                            int tileX = chunkX + localX;
                            int tileY = chunkY + localY;
                            gids.put(key(tileX, tileY), gid);

                            // O Tiled exporta chunks maiores que a área realmente
                            // desenhada e preenche o restante com GID 0. Esses
                            // tiles vazios NÃO fazem parte do mundo jogável.
                            // Considerar somente GIDs válidos mantém a origem
                            // do mapa alinhada com a máscara de colisão.
                            if (gid > 0) {
                                parsedMinX = Math.min(parsedMinX, tileX);
                                parsedMinY = Math.min(parsedMinY, tileY);
                                parsedMaxX = Math.max(parsedMaxX, tileX);
                                parsedMaxY = Math.max(parsedMaxY, tileY);
                            }
                        }
                    }
                }

                if (parsedMinX == Integer.MAX_VALUE) {
                    parsedMinX = 0;
                    parsedMinY = 0;
                    parsedMaxX = width - 1;
                    parsedMaxY = height - 1;
                }
            } else {
                // Mapa finito: os GIDs ficam diretamente na grade width x height.
                String[] values = data.getTextContent()
                        .replaceAll("\\s+", "")
                        .trim()
                        .split(",");

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int index = y * width + x;
                        if (index < values.length && !values[index].isEmpty()) {
                            gids.put(key(x, y), Integer.parseInt(values[index]));
                        }
                    }
                }
            }

            minTileX = parsedMinX;
            minTileY = parsedMinY;
            maxTileX = parsedMaxX;
            maxTileY = parsedMaxY;
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw (IllegalStateException) e;
            throw new IllegalStateException("Falha ao carregar mapa Tiled: " + resourcePath, e);
        }
    }

    private static int parseIntAttribute(Element element, String attribute, String resourcePath) {
        String value = element.getAttribute(attribute);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Atributo '" + attribute + "' ausente no mapa: " + resourcePath);
        }
        return Integer.parseInt(value);
    }

    private static long key(int x, int y) {
        return ((long) x << 32) ^ (y & 0xffffffffL);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getMinTileX() { return minTileX; }
    public int getMinTileY() { return minTileY; }
    public int getMaxTileX() { return maxTileX; }
    public int getMaxTileY() { return maxTileY; }

    public double getPixelWidth() {
        return (maxTileX - minTileX + 1) * tileSet.tileWidth();
    }

    public double getPixelHeight() {
        return (maxTileY - minTileY + 1) * tileSet.tileHeight();
    }

    public TileSet getTileSet() { return tileSet; }

    public int getGid(int x, int y) {
        return gids.getOrDefault(key(x, y), 0);
    }

    /**
     * Lê objetos de uma camada do Tiled e converte as coordenadas originais
     * do mapa infinito para as coordenadas normalizadas usadas pelo jogo.
     */
    public java.util.List<MapObject> getObjects(String layerName) {
        java.util.List<MapObject> result = new java.util.ArrayList<>();

        try (InputStream stream = getClass().getResourceAsStream(currentResourcePath)) {
            if (stream == null) return result;

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(stream);

            NodeList groups = doc.getElementsByTagName("objectgroup");
            double originX = minTileX * tileSet.tileWidth();
            double originY = minTileY * tileSet.tileHeight();

            for (int i = 0; i < groups.getLength(); i++) {
                Element group = (Element) groups.item(i);
                if (!layerName.equalsIgnoreCase(group.getAttribute("name"))) continue;

                NodeList objects = group.getElementsByTagName("object");
                for (int j = 0; j < objects.getLength(); j++) {
                    Element object = (Element) objects.item(j);
                    double x = parseDouble(object.getAttribute("x")) - originX;
                    double y = parseDouble(object.getAttribute("y")) - originY;
                    double width = parseDouble(object.getAttribute("width"));
                    double height = parseDouble(object.getAttribute("height"));
                    double rotation = parseDouble(object.getAttribute("rotation"));

                    result.add(new MapObject(x, y, width, height, rotation));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao ler camada '" + layerName + "' do Tiled.", e);
        }

        return result;
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0;
        return Double.parseDouble(value);
    }

}
