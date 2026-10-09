package com.agroroute.core.theme

import androidx.compose.ui.graphics.Brush

/** Short, high contrast feature surfaces; long-form content stays on solid surfaces. */
object AgroGradients {
    val forest get() = Brush.linearGradient(listOf(ForestDeep, ForestMid, ForestSoft))
    val earth get() = Brush.linearGradient(listOf(SoilBrown, ClaySurface))
    val botanical get() = Brush.linearGradient(listOf(ForestMid, FreshGreen))
    val champagne get() = Brush.linearGradient(listOf(WarmSand, Champagne))
    val night get() = Brush.linearGradient(listOf(NightForest, CharcoalForest))
    val avatar get() = Brush.linearGradient(listOf(LeafAccent, Champagne, ForestSoft))
}
