package com.example.leitordocumento_compose.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// ARCHITECTURAL ARCHIVIST — Design System Colors
// Semente: Azul Profundo #003461
// =============================================================================

// -----------------------------------------------------------------------------
// PRIMÁRIA — Âncora Azul Profundo
// -----------------------------------------------------------------------------
val Primaria               = Color(0xFF003461)  // primary
val SobrePrimaria          = Color(0xFFFFFFFF)  // on-primary
val ContainerPrimaria      = Color(0xFF004B87)  // primary-container (alvo do gradiente)
val SobreContainerPrimaria = Color(0xFFD6E8FF)  // on-primary-container
val PrimariaFixaEscura     = Color(0xFFA3C9FF)  // primary-fixed-dim (destaques dark mode)

// -----------------------------------------------------------------------------
// SECUNDÁRIA
// -----------------------------------------------------------------------------
val Secundaria               = Color(0xFF4A6077)
val SobreSecundaria          = Color(0xFFFFFFFF)
val ContainerSecundaria      = Color(0xFFCDE5FF)
val SobreContainerSecundaria = Color(0xFF001E30)

// -----------------------------------------------------------------------------
// TERCIÁRIA
// -----------------------------------------------------------------------------
val Terciaria               = Color(0xFF5A5C7E)
val SobreTerciaria          = Color(0xFFFFFFFF)
val ContainerTerciaria      = Color(0xFFE1E0FF)
val SobreContainerTerciaria = Color(0xFF171837)

// -----------------------------------------------------------------------------
// ERRO
// -----------------------------------------------------------------------------
val Erro               = Color(0xFFB3261E)
val SobreErro          = Color(0xFFFFFFFF)
val ContainerErro      = Color(0xFFF9DEDC)
val SobreContainerErro = Color(0xFF410E0B)

// -----------------------------------------------------------------------------
// HIERARQUIA DE SUPERFÍCIES — "Princípio do Empilhamento"
// Usar variações tonais de fundo em vez de bordas 1px (Regra Sem-Linha)
// -----------------------------------------------------------------------------
val Fundo      = Color(0xFFF9F9FF)  // camada base / background
val SobreFundo = Color(0xFF191C20)  // on-background

val Superficie              = Color(0xFFF9F9FF)  // camada base
val SobreSuperficie         = Color(0xFF191C20)  // on-surface — títulos
val VarianteSuperficie      = Color(0xFFDDE3EA)  // surface-variant
val SobreVarianteSuperficie = Color(0xFF424750)  // on-surface-variant — corpo de texto

val ContainerSuperficieMaisAbaixo = Color(0xFFFFFFFF)  // cards interativos
val ContainerSuperficieAbaixo     = Color(0xFFF3F3F9)  // seções estruturais
val ContainerSuperficie           = Color(0xFFEDEEF4)  // container padrão
val ContainerSuperficieAcima      = Color(0xFFE7E8ED)  // fundos elevados
val ContainerSuperficieMaisAcima  = Color(0xFFE1E2E8)  // campos de input, hover secundário

// -----------------------------------------------------------------------------
// CONTORNO — Filosofia "Borda Fantasma"
// Usar variante de contorno a 20% de opacidade para linhas sutis de acessibilidade
// Nunca usar contorno em 100% exceto para acessibilidade de alto contraste
// -----------------------------------------------------------------------------
val Contorno         = Color(0xFF727781)  // apenas para a11y de alto contraste
val VarianteContorno = Color(0xFFC2C6D1)  // base da borda fantasma — aplicar a 0.20f alpha

// -----------------------------------------------------------------------------
// ESPECIAIS / TINTE DE SUPERFÍCIE
// -----------------------------------------------------------------------------
val TinteSuperficie        = Primaria
val SuperficieInversa      = Color(0xFF2E3135)
val SobreSuperficieInversa = Color(0xFFF0F1F6)
val PrimariaInversa        = Color(0xFFA3C9FF)

val Veu    = Color(0xFF000000)  // scrim
val Sombra = Primaria           // sombras com tinte primário a ~6% de opacidade (nunca preto puro)

// =============================================================================
// TEMA ESCURO
// =============================================================================

val PrimariaEscuro               = Color(0xFFA3C9FF)  // primary-fixed-dim como primary no dark
val SobrePrimariaEscuro          = Color(0xFF00315E)
val ContainerPrimariaEscuro      = Color(0xFF004B87)
val SobreContainerPrimariaEscuro = Color(0xFFD6E8FF)

val SecundariaEscuro               = Color(0xFFB1CAE0)
val SobreSecundariaEscuro          = Color(0xFF1C3547)
val ContainerSecundariaEscuro      = Color(0xFF32495E)
val SobreContainerSecundariaEscuro = Color(0xFFCDE5FF)

val TerciariaEscuro               = Color(0xFFC4C3EB)
val SobreTerciariaEscuro          = Color(0xFF2C2D4D)
val ContainerTerciariaEscuro      = Color(0xFF424465)
val SobreContainerTerciariaEscuro = Color(0xFFE1E0FF)

val ErroEscuro               = Color(0xFFF2B8B5)
val SobreErroEscuro          = Color(0xFF601410)
val ContainerErroEscuro      = Color(0xFF8C1D18)
val SobreContainerErroEscuro = Color(0xFFF9DEDC)

val FundoEscuro      = Color(0xFF111318)
val SobreFundoEscuro = Color(0xFFE2E2E9)

val SuperficieEscuro              = Color(0xFF111318)
val SobreSuperficieEscuro         = Color(0xFFE2E2E9)
val VarianteSuperficieEscuro      = Color(0xFF424750)
val SobreVarianteSuperficieEscuro = Color(0xFFC2C6D1)

val ContainerSuperficieMaisAbaixoEscuro = Color(0xFF0C0E13)
val ContainerSuperficieAbaixoEscuro     = Color(0xFF191C20)
val ContainerSuperficieEscuro           = Color(0xFF1D2024)
val ContainerSuperficieAcimaEscuro      = Color(0xFF282A2F)
val ContainerSuperficieMaisAcimaEscuro  = Color(0xFF323539)

val ContornoEscuro         = Color(0xFF8C9099)
val VarianteContornoEscuro = Color(0xFF424750)

val SuperficieInversaEscuro      = Color(0xFFE2E2E9)
val SobreSuperficieInversaEscuro = Color(0xFF2E3135)
val PrimariaInversaEscuro        = Color(0xFF003461)
val TinteSuperficieEscuro        = PrimariaEscuro