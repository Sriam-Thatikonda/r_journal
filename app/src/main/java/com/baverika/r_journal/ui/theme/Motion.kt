package com.baverika.r_journal.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavBackStackEntry

object MotionSpec {
    const val TAB_DURATION = 240
    const val DRAWER_DURATION = 260
    const val PUSH_DURATION = 320
    const val MODAL_DURATION = 300
    const val VIEWER_DURATION = 260

    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing = FastOutSlowInEasing

    fun <T> snappySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> tactileSpring() = spring<T>(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * Subtle spring press-scale feedback for tactile cards, pills, and action buttons.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource? = null,
    pressedScale: Float = 0.965f,
    enabled: Boolean = true
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = MotionSpec.tactileSpring(),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Route classification for motion choreography across R-Journal.
 */
enum class RouteMotionType {
    DOCK_TAB,
    DRAWER_HUB,
    DETAIL_PUSH,
    MODAL_SHEET,
    MEDIA_VIEWER
}

object RouteTransitions {
    private val dockOrder = mapOf(
        "archive" to 0,
        "quick_notes" to 1,
        "tasks" to 2
    )

    private val drawerRoutes = setOf(
        "dashboard",
        "habits",
        "challenges",
        "craving_quest",
        "trackers",
        "calendar",
        "events",
        "life_trackers",
        "quotes",
        "password_generator",
        "chat_input/mine",
        "settings",
        "export",
        "import"
    )

    private val modalPrefixes = listOf(
        "new_quick_note",
        "edit_quick_note",
        "add_task",
        "edit_task",
        "add_habit",
        "add_tracker",
        "edit_tracker",
        "add_craving",
        "create_challenge"
    )

    fun classify(route: String?): RouteMotionType {
        if (route == null) return RouteMotionType.DRAWER_HUB
        if (route in dockOrder) return RouteMotionType.DOCK_TAB
        if (route.startsWith("image_viewer")) return RouteMotionType.MEDIA_VIEWER
        if (modalPrefixes.any { route == it || route.startsWith("$it/") || route.startsWith("$it?") }) {
            return RouteMotionType.MODAL_SHEET
        }
        if (route in drawerRoutes) return RouteMotionType.DRAWER_HUB
        return RouteMotionType.DETAIL_PUSH
    }

    fun enterTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition {
        val fromRoute = scope.initialState.destination.route
        val toRoute = scope.targetState.destination.route
        val fromType = classify(fromRoute)
        val toType = classify(toRoute)

        return when {
            fromType == RouteMotionType.DOCK_TAB && toType == RouteMotionType.DOCK_TAB -> {
                val forward = (dockOrder[toRoute] ?: 0) >= (dockOrder[fromRoute] ?: 0)
                val sign = if (forward) 1 else -1
                slideInHorizontally(
                    animationSpec = tween(MotionSpec.TAB_DURATION, easing = MotionSpec.EmphasizedDecelerate),
                    initialOffsetX = { (it * 0.10f * sign).toInt() }
                ) + fadeIn(tween(MotionSpec.TAB_DURATION, easing = MotionSpec.StandardEasing))
            }

            toType == RouteMotionType.MEDIA_VIEWER -> {
                scaleIn(
                    initialScale = 0.90f,
                    animationSpec = tween(MotionSpec.VIEWER_DURATION, easing = MotionSpec.EmphasizedDecelerate)
                ) + fadeIn(tween(MotionSpec.VIEWER_DURATION))
            }

            toType == RouteMotionType.MODAL_SHEET -> {
                slideInVertically(
                    animationSpec = tween(MotionSpec.MODAL_DURATION, easing = MotionSpec.EmphasizedDecelerate),
                    initialOffsetY = { (it * 0.16f).toInt() }
                ) + fadeIn(tween(MotionSpec.MODAL_DURATION))
            }

            toType == RouteMotionType.DETAIL_PUSH -> {
                slideInHorizontally(
                    animationSpec = tween(MotionSpec.PUSH_DURATION, easing = MotionSpec.EmphasizedDecelerate),
                    initialOffsetX = { (it * 0.22f).toInt() }
                ) + fadeIn(tween(MotionSpec.PUSH_DURATION, easing = MotionSpec.StandardEasing))
            }

            else -> {
                scaleIn(
                    initialScale = 0.965f,
                    animationSpec = tween(MotionSpec.DRAWER_DURATION, easing = MotionSpec.EmphasizedDecelerate)
                ) + fadeIn(tween(MotionSpec.DRAWER_DURATION))
            }
        }
    }

    fun exitTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition {
        val fromRoute = scope.initialState.destination.route
        val toRoute = scope.targetState.destination.route
        val fromType = classify(fromRoute)
        val toType = classify(toRoute)

        return when {
            fromType == RouteMotionType.DOCK_TAB && toType == RouteMotionType.DOCK_TAB -> {
                val forward = (dockOrder[toRoute] ?: 0) >= (dockOrder[fromRoute] ?: 0)
                val sign = if (forward) -1 else 1
                slideOutHorizontally(
                    animationSpec = tween(MotionSpec.TAB_DURATION, easing = MotionSpec.StandardEasing),
                    targetOffsetX = { (it * 0.08f * sign).toInt() }
                ) + fadeOut(tween(180))
            }

            toType == RouteMotionType.MEDIA_VIEWER -> {
                fadeOut(tween(MotionSpec.VIEWER_DURATION))
            }

            toType == RouteMotionType.MODAL_SHEET -> {
                scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(MotionSpec.MODAL_DURATION, easing = MotionSpec.StandardEasing)
                ) + fadeOut(tween(220))
            }

            toType == RouteMotionType.DETAIL_PUSH -> {
                slideOutHorizontally(
                    animationSpec = tween(MotionSpec.PUSH_DURATION, easing = MotionSpec.StandardEasing),
                    targetOffsetX = { -(it * 0.08f).toInt() }
                ) + fadeOut(tween(220))
            }

            else -> {
                fadeOut(tween(180))
            }
        }
    }

    fun popEnterTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition {
        val fromRoute = scope.initialState.destination.route
        val fromType = classify(fromRoute)

        return when (fromType) {
            RouteMotionType.MEDIA_VIEWER -> {
                fadeIn(tween(MotionSpec.VIEWER_DURATION))
            }
            RouteMotionType.MODAL_SHEET -> {
                scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(MotionSpec.MODAL_DURATION, easing = MotionSpec.EmphasizedDecelerate)
                ) + fadeIn(tween(MotionSpec.MODAL_DURATION))
            }
            RouteMotionType.DETAIL_PUSH -> {
                slideInHorizontally(
                    animationSpec = tween(MotionSpec.PUSH_DURATION, easing = MotionSpec.EmphasizedDecelerate),
                    initialOffsetX = { -(it * 0.08f).toInt() }
                ) + fadeIn(tween(MotionSpec.PUSH_DURATION))
            }
            else -> {
                scaleIn(
                    initialScale = 0.97f,
                    animationSpec = tween(MotionSpec.DRAWER_DURATION, easing = MotionSpec.EmphasizedDecelerate)
                ) + fadeIn(tween(MotionSpec.DRAWER_DURATION))
            }
        }
    }

    fun popExitTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition {
        val fromRoute = scope.initialState.destination.route
        val fromType = classify(fromRoute)

        return when (fromType) {
            RouteMotionType.MEDIA_VIEWER -> {
                scaleOut(
                    targetScale = 0.90f,
                    animationSpec = tween(MotionSpec.VIEWER_DURATION, easing = MotionSpec.EmphasizedAccelerate)
                ) + fadeOut(tween(200))
            }
            RouteMotionType.MODAL_SHEET -> {
                slideOutVertically(
                    animationSpec = tween(MotionSpec.MODAL_DURATION, easing = MotionSpec.EmphasizedAccelerate),
                    targetOffsetY = { (it * 0.16f).toInt() }
                ) + fadeOut(tween(220))
            }
            RouteMotionType.DETAIL_PUSH -> {
                slideOutHorizontally(
                    animationSpec = tween(MotionSpec.PUSH_DURATION, easing = MotionSpec.EmphasizedAccelerate),
                    targetOffsetX = { (it * 0.22f).toInt() }
                ) + fadeOut(tween(220))
            }
            else -> {
                fadeOut(tween(180))
            }
        }
    }
}
