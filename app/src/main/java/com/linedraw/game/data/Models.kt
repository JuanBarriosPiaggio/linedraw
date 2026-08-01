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
 * @param gridSize side length of the square grid the dots live on (3..6)
 * @param dots all dots that must be visited
 * @param edges valid connections as pairs of dot ids; the player may only draw along these
 * @param solution one known valid path (list of dot ids) that visits every dot —
 *        used by the level validator test and as a fallback for the hint system
 */
@Serializable
data class Level(
    val id: Int,
    val gridSize: Int,
    val dots: List<Dot>,
    val edges: List<List<Int>>,
    val solution: List<Int> = emptyList(),
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
