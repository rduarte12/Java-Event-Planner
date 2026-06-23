# Diagrama de Classes — UML

Diagrama de classes geral da aplicação Java Event Planner.

## Arquivos
- `class-diagram.puml` — fonte (PlantUML).
- `class-diagram.png` — imagem rasterizada (para colar no relatório/slides).
- `class-diagram.svg` — versão vetorial (zoom sem perda de qualidade).

## Como gerar de novo

Precisa de Java + `plantuml.jar`.

```bash
java -DPLANTUML_LIMIT_SIZE=16384 -jar plantuml.jar -tpng -tsvg class-diagram.puml
```

> O diagrama tem ~4600px de largura. O PlantUML corta imagens acima de
> 4096px por padrão, então a flag `-DPLANTUML_LIMIT_SIZE=16384` é
> obrigatória para o PNG sair inteiro. O SVG nunca é cortado.

## Convenções usadas
- Seta cheia com triângulo vazado: herança / implementação de interface.
- Losango preenchido (`*-->`): composição (posse forte, ex.: `MainWindow` e suas views).
- Losango vazado (`o-->`): agregação (ex.: DAOs e suas listas em memória).
- Linha tracejada (`..>`): dependência / uso.
- `«utility»`: classe só de métodos estáticos. `«entry point»`: classe `main`.
