package com.linedraw.game.data

import kotlinx.serialization.Serializable

@Serializable
data class Dot(
    val id: Int,
    val x: Int,
    val y: Int,
)

/**
 * A puzzle level.
 *
 * @param gridSize extent of the coordinate space the dots live on (square levels
 *        use a 3..6 lattice; shape levels use a finer virtual grid, e.g. 13)
 * @param dots all dots that must be visited; positions are arbitrary within the grid space
 * @param edges valid connections as pairs of dot ids; the player may only draw along these
 * @param solution one known valid path (list of dot ids) that visits every dot —
 *        used by the level validator test and as a fallback for the hint system
 * @param name optional display name for special shape levels (e.g. "Star", "Fish")
 */
@Serializable
data class Level(
    val id: Int,
    val gridSize: Int,
    val dots: List<Dot>,
    val edges: List<List<Int>>,
    val solution: List<Int> = emptyList(),
    val name: String? = null,
)

@Serializable
data class LevelPack(
    val levels: List<Level>,
)

/** Player progress for a single level. */
data class LevelProgress(
    val id: Int,
    val unlocked: Boolean,
    val stars: Int,
)
