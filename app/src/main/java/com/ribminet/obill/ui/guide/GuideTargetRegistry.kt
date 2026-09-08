package com.ribminet.obill.ui.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

class GuideTargetRegistry {
    private val _bounds = mutableStateMapOf<GuideTarget, Rect>()
    val bounds: Map<GuideTarget, Rect> get() = _bounds

    fun update(target: GuideTarget, rect: Rect) {
        val prev = _bounds[target]
        if (prev == null ||
            prev.left != rect.left ||
            prev.top != rect.top ||
            prev.right != rect.right ||
            prev.bottom != rect.bottom
        ) {
            _bounds[target] = rect
        }
    }

    fun clear(target: GuideTarget) {
        _bounds.remove(target)
    }
}

val LocalGuideTargetRegistry = staticCompositionLocalOf { GuideTargetRegistry() }

@Composable
fun rememberGuideTargetRegistry(): GuideTargetRegistry = remember { GuideTargetRegistry() }

fun Modifier.guideTarget(target: GuideTarget): Modifier = composed {
    val registry = LocalGuideTargetRegistry.current
    DisposableEffect(target) {
        onDispose { registry.clear(target) }
    }
    onGloballyPositioned { coords ->
        registry.update(target, coords.boundsInRoot())
    }
}
