# Flash Mod (Fabric 1.21.1)

Mod de velocista para Minecraft 1.21.1 (Fabric, Java 21): super velocidade, correr em paredes,
raios coloridos e menu para escolher a cor do raio.

## Controles (podem ser mudados em Opções > Controles > Flash Mod)

| Tecla | Ação |
|-------|------|
| V | Liga/desliga a Força da Velocidade |
| B | Muda o nível de velocidade (1, 2, 3) |
| K | Abre o menu da cor do raio |

## O que tem na versão 1.0

- 3 níveis de velocidade, com barra de energia (Speed Force) na tela.
- Correr na parede: andando para frente contra um bloco, você sobe.
- Sem dano de queda enquanto o poder está ligado.
- Rastro de raios com núcleo e brilho, visível para os outros jogadores.
- Linhas de velocidade nas bordas da tela (níveis 2 e 3), na cor do raio.
- Menu de cor: 8 cores prontas, sliders RGB, código hexadecimal, modo arco-íris e prévia do raio.
- A cor fica salva em `config/flashmod.properties`.

## Como compilar pelo GitHub Actions

1. Crie um repositório no GitHub e envie todos os arquivos desta pasta.
2. Abra a aba **Actions**, rode **Build Flash Mod** (ou faça um push).
3. Baixe o artefato **flashmod** e use o `flashmod-1.0.0.jar` (não o `-sources`) na pasta `mods`,
   junto com o **Fabric API** para 1.21.1.

Para compilar no PC: `gradle build` (precisa de Gradle 8.8+ e Java 21).

## Ajustes rápidos

Velocidades, gasto e recarga de energia ficam em `client/FlashClientState.java`.
Cores prontas ficam em `client/ColorScreen.java` e os textos em `assets/flashmod/lang/`.

## Próximas etapas sugeridas

- Traje do Flash (armadura com textura e modelo).
- Imagens residuais (afterimages) atrás do jogador.
- Correr na água, atravessar blocos (fase) e socos rápidos.
- Tempo lento ao redor.
