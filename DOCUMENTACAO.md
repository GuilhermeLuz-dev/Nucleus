# Documentação da navegação do mapa e da IA

## 1) Como o tilemap foi convertido em uma estrutura navegável

O ponto central da navegação está em AStarPathfinder.java

A conversão funciona em duas camadas:

- O Tiled é carregado em TiledMap.java. Esse arquivo lê o arquivo `.tmx`, identifica o tileset, as dimensões do mapa, os chunks e os objetos das camadas do editor.
- A colisão é validada em CollisionMap.java. A classe interpreta a camada `Colisão` do Tiled, converte as coordenadas para o espaço do jogo e transforma retângulos/polígonos em áreas bloqueadas.

A estrutura navegável é criada em runtime por um grid de células discretas:

- A constante `CELL = 24.0` define o tamanho da célula do mapa;
- `worldToCell()` e `cellToWorld()` convertem coordenadas do mundo em índices de grade e vice-versa;
- `isWalkable(...)` avalia se uma célula pode ser ocupada verificando uma pequena área dos pés do inimigo com o `CollisionMap.canMoveTo(...)`.

Em outras palavras, o mapa não vira um grafo explícito manualmente. Ele vira um espaço de busca em grade em que cada célula é considerada livre ou bloqueada com base na colisão real do cenário. O algoritmo usa essa informação para descobrir um caminho.

### Regras importantes da navegação

- A busca considera os 8 vizinhos possíveis: horizontal, vertical e diagonal.
- Quando a movimentação é diagonal, o algoritmo só permite a passagem se os dois vizinhos ortogonais também forem transitáveis.
- Isso evita que o agente corte cantos de parede e atravesse obstáculos em ângulos inválidos.

Essa regra está implementada no loop de expansão do A\* em AStarPathfinder.java

---

## 2) Qual função heurística foi utilizada

A função heurística usada no A\* é a distância octil (octile distance), implementada em:

- AStarPathfinder.java

A função é:

```java
private double heuristic(int x, int y, int gx, int gy) {
    int dx = Math.abs(gx - x);
    int dy = Math.abs(gy - y);
    int diagonal = Math.min(dx, dy);
    int straight = Math.max(dx, dy) - diagonal;
    return diagonal * Math.sqrt(2.0) + straight;
}
```

Interpretando matematicamente:

- `diagonal * sqrt(2)` representa o custo de movimento em diagonal;
- `straight` representa os passos restantes em linha reta;
- a fórmula equivale ao custo octil clássico, que é a escolha adequada em grades com movimento em 8 direções.

Em notação compacta, a heurística é:

$$
h(n) = d_{diag} \cdot \sqrt{2} + (d_{straight} - d_{diag})
$$

onde:

- $d_{diag}$ = menor distância em diagonais;
- $d_{straight}$ = diferença total em linhas retas após descontar a diagonal.

Essa heurística é admissível e consistente para um ambiente com movimento livre em 8 direções.

---

## 3) Quais movimentos são permitidos ao agente

Os movimentos do agente estão em Enemy.java e seguem estes pontos:

### 3.1. Movimento do agente por busca A\*

No `AStarPathfinder`, o conjunto de direções é:

```java
private static final int[][] DIRECTIONS = {
    { 1, 0}, {-1, 0}, {0, 1}, {0, -1},
    { 1, 1}, { 1,-1}, {-1, 1}, {-1,-1}
};
```

Isso significa que o agente pode andar em:

- Norte
- Sul
- Leste
- Oeste
- Nordeste
- Noroeste
- Sudeste
- Sudoeste

### 3.2. Restrições de movimento

O agente NÃO pode:

- atravessar células bloqueadas pela colisão;
- cortar diagonalmente cantos de obstáculo sem que os caminhos ortogonais também estejam livres.

Essa regra é essencial para evitar que o inimigo passe por paredes ou fique preso.

### 3.3. Movimento final do agente

Após o caminho ser calculado, o agente segue cada ponto do caminho por `moveTowards(...)`, que aplica:

- deslocamento em direção ao waypoint;
- verificação de ocupação por `canOccupy(...)`;
- fallback de desvio lateral quando a direção direta está bloqueada.

Ou seja, o agente tem movimentação contínua e também um ajuste local para escapar de travamentos.

---

## 4) Como executar e testar a funcionalidade

### Requisitos

- Java 21
- Maven
- JavaFX

O projeto usa o plugin JavaFX em [pom.xml](pom.xml), com a classe principal:

- `br.edu.unex.nucleus.app.GameApplication`

### Compilar

No diretório do projeto, execute:

```bash
mvn -q -DskipTests compile
```

Validação feita no ambiente atual: este comando retornou sucesso, com saída vazia e sem erros de compilação.

### Executar a aplicação

```bash
mvn javafx:run
```

Esse comando inicia a janela principal do jogo e carrega os mapas criados em Tiled.

### Testar a funcionalidade de navegação

Para verificar o comportamento da IA:

1. execute a aplicação;
2. avance para um mapa com inimigos comuns;
3. mova o jogador para longe do ponto inicial ou para uma área com obstáculos;
4. observe se os inimigos:
   - perseguem pela rota calculada;
   - evitam paredes e cantos;
   - recalculam o caminho quando o alvo muda de posição;
   - retornam ao ponto de origem quando perdem o alvo.

### Ponto importante

A lógica de navegação está integrada no fluxo de atualização da IA em Enemy.java, e não em uma interface separada. O agente recalcula o caminho periodicamente e usa o `CollisionMap` como referência de ocupabilidade para decidir se cada célula é válida.

---

## Resumo

A arquitetura atual combina:

- TiledMap.java: leitura do mapa do Tiled;
- CollisionMap.java: colisão e área bloqueada;
- AStarPathfinder.java: busca de caminho em grade;
- Enemy.java: uso do A\* na IA do inimigo.

O resultado é um agente com movimentação em 8 direções, heurística octil e navegação baseada em colisão real do mapa.
