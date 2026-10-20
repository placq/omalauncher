package dev.omalauncher.ui

import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/** Prototype: mGBA's SDL window is embedded like foot, not opened in a task. */
@Composable
fun GbaView(term: EmbeddedTerminal, modifier: Modifier = Modifier) {
    val view = LocalView.current
    DisposableEffect(term, view) {
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            if (!focused) term.pauseGame()
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose {
            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
            term.pauseGame()
        }
    }
    Column(modifier.background(Color(0xff39325f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText("POCKET GBA · PROTOTYPE", style = Mono.copy(color = Color.White))
        TerminalView(
            term,
            Modifier.fillMaxWidth().aspectRatio(1.5f),
            interactive = false,
            gameDisplay = true,
            autoStart = false,
        )
        Box(
            Modifier.fillMaxWidth().height(44.dp).background(Palette.surface)
                .clickable {
                    if (term.window == null) term.start()
                    else if (term.gamePlaying) term.pauseGame() else term.resumeGame()
                },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(if (term.gamePlaying) "PAUZA" else "GRAJ", style = Mono)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GbaKey(term, "L", 30)
            GbaKey(term, "R", 31)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GbaKey(term, "↑", 103)
                Row {
                    GbaKey(term, "←", 105)
                    Box(Modifier.size(48.dp))
                    GbaKey(term, "→", 106)
                }
                GbaKey(term, "↓", 108)
            }
            Column(horizontalAlignment = Alignment.End) {
                GbaKey(term, "A", 45) // SDL defaults: X = A, Z = B.
                Row {
                    GbaKey(term, "B", 44)
                    Box(Modifier.size(24.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            GbaKey(term, "SEL", 14)
            GbaKey(term, "START", 28)
        }
        BasicText("ROM-y i zapisy: ~/Games/GBA\nPrzesuń stronę poza przyciskami.", style = Mono.copy(color = Color(0xffd2cde6), fontSize = TextSize.Small))
    }
}

/** Independent pointer scopes allow direction + A/B and release on cancellation. */
@Composable
private fun GbaKey(term: EmbeddedTerminal, label: String, code: Int) {
    var pressed by remember(term, code) { mutableStateOf(false) }
    Box(
        Modifier.padding(2.dp).size(48.dp)
            .background(if (pressed) Palette.accent else Color(0xff191725), RoundedCornerShape(8.dp))
            .pointerInput(term, code, term.gamePlaying) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    down.consume()
                    pressed = true
                    term.gameKey(code, true)
                    try {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pointer = event.changes.firstOrNull { it.id == down.id }
                            event.changes.forEach { it.consume() }
                        } while (pointer != null && pointer.pressed)
                    } finally {
                        term.gameKey(code, false)
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = Mono.copy(color = Color.White, fontSize = TextSize.Small))
    }
}
