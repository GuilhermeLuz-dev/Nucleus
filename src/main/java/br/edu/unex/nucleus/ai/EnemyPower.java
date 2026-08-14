package br.edu.unex.nucleus.ai;

import br.edu.unex.nucleus.entity.Player;
import br.edu.unex.nucleus.entity.Projectile;

import java.util.List;

public interface EnemyPower {

    /**
     * Cria os projéteis desse poder a partir da posição do inimigo,
     * mirando (ou não) no jogador. Cada implementação decide sua própria
     * velocidade, alcance e quantidade de projéteis.
     */
    List<Projectile> cast(double originX, double originY, Player player);
}
