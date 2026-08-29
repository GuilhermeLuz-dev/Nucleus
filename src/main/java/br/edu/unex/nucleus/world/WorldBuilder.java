package br.edu.unex.nucleus.world;

import br.edu.unex.nucleus.ai.EnemyPower;
import br.edu.unex.nucleus.ai.HomingOrbPower;
import br.edu.unex.nucleus.ai.SingleBoltPower;
import br.edu.unex.nucleus.ai.TripleBoltPower;
import br.edu.unex.nucleus.entity.Enemy;
import br.edu.unex.nucleus.entity.HealthPotion;
import br.edu.unex.nucleus.entity.Portal;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Fábrica da campanha. O GameEngine não precisa conhecer os detalhes dos
 * arquivos .tmx; ele apenas recebe um WorldManager pronto.
 */
public final class WorldBuilder {

    private WorldBuilder() {}

    public static WorldManager createCampaign() {
        WorldManager world = new WorldManager();

        world.addIsland(createIsland(
                "terrak",
                "Terrak — O Vale dos Colossos",
                "/backgrounds/terrak.tmx",
                Color.rgb(34, 65, 30),
                new TerrakBoss(615, 135)
        ));

        world.addIsland(createIsland(
                "nerion",
                "Nerion — O Abismo Azul",
                "/backgrounds/nerion.tmx",
                Color.rgb(15, 80, 120),
                new NerionBoss(615, 135)
        ));

        world.addIsland(createIsland(
                "ignar",
                "Ignar — A Forja Infernal",
                "/backgrounds/ignar.tmx",
                Color.rgb(120, 35, 15),
                new IgnarBoss(615, 135)
        ));

        world.setInitialIsland("terrak");
        world.initializeEntitiesFromMaps();
        return world;
    }

    private static Island createIsland(
            String id,
            String displayName,
            String mapPath,
            Color fallbackColor,
            Enemy fallbackBoss) {

        Island island = new Island(
                id,
                displayName,
                new ArrayList<>(),
                new ArrayList<>(),
                fallbackColor,
                null,
                mapPath
        );

        island.setFallbackBoss(fallbackBoss);
        return island;
    }
}
