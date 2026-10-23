package app.accordgarage.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.accordgarage.content.Safety
import app.accordgarage.content.Tutorial
import app.accordgarage.content.Tutorials
import app.accordgarage.data.StepCheck
import app.accordgarage.data.TutorialNote
import app.accordgarage.domain.CarSystem
import app.accordgarage.ui.LocalContainer
import app.accordgarage.ui.components.Field
import app.accordgarage.ui.components.GarageTopBar
import app.accordgarage.ui.components.Note
import app.accordgarage.ui.components.Panel
import app.accordgarage.ui.components.PrimaryButton
import app.accordgarage.ui.components.RowItem
import app.accordgarage.ui.components.SecondaryButton
import app.accordgarage.ui.components.SectionLabel
import app.accordgarage.ui.components.Tag
import app.accordgarage.ui.components.VSpace
import app.accordgarage.ui.manualSearch
import app.accordgarage.ui.openLink
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun safetyColor(s: Safety) = when (s) {
    Safety.GREEN -> LocalGarage.current.green
    Safety.AMBER -> LocalGarage.current.amber
    Safety.RED -> LocalGarage.current.accent
}

@Composable
fun GuidesScreen(nav: NavHostController) {
    val g = LocalGarage.current
    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Guides", "Written for this car: 2.4L, automatic, coupe")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            val featured = Tutorials.get("head_unit")
            if (featured != null) {
                VSpace(4)
                Panel(Modifier.fillMaxWidth(), onClick = { nav.navigate("tutorial/${featured.id}") }) {
                    Column {
                        Text("FEATURED", style = Type.label, color = g.accent)
                        VSpace(4)
                        Text(featured.title, style = Type.heading, color = g.text)
                        VSpace(4)
                        Text("${featured.steps.size} steps · with the install videos", style = Type.monoSmall, color = g.muted)
                    }
                }
            }
            CarSystem.entries.forEach { system ->
                val list = Tutorials.all.filter { it.system == system && it.id != "head_unit" }
                if (list.isNotEmpty()) {
                    VSpace(16)
                    SectionLabel(system.label)
                    list.forEach { t ->
                        RowItem(
                            t.title,
                            "DIFFICULTY ${t.difficulty}/5 · ~${t.minutes} MIN · ${t.safety.label}",
                            onClick = { nav.navigate("tutorial/${t.id}") },
                            dot = safetyColor(t.safety),
                        )
                    }
                }
            }
            VSpace(32)
        }
    }
}

@Composable
fun TutorialScreen(nav: NavHostController, id: String) {
    val t = Tutorials.get(id) ?: return
    val c = LocalContainer.current
    val g = LocalGarage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stepIndex by remember { mutableStateOf(-1) } // -1 = overview
    val checks by c.db.settings().checks("$id:").collectAsState(initial = emptyList())
    val notes by c.db.settings().notes(id).collectAsState(initial = emptyList())
    var readAloud by remember { mutableStateOf(false) }

    // Keep the screen on while working.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.setLanguage(Locale.US)
                tts = engine
            }
        }
        onDispose { engine?.shutdown() }
    }
    LaunchedEffect(stepIndex, readAloud, tts) {
        val engine = tts ?: return@LaunchedEffect
        if (!readAloud) { engine.stop(); return@LaunchedEffect }
        val text = if (stepIndex < 0) "${t.title}. ${t.summary}"
        else t.steps[stepIndex].let { s -> "Step ${stepIndex + 1}. ${s.title}. ${s.body} ${s.substeps.joinToString(". ")} ${s.caution ?: ""}" }
        delay(150)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "step")
    }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar(t.system.label, t.title, onBack = { nav.popBackStack() }) {
            IconButton(onClick = { readAloud = !readAloud }) {
                Icon(
                    if (readAloud) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                    contentDescription = if (readAloud) "Stop reading aloud" else "Read steps aloud",
                    tint = if (readAloud) g.accent else g.text2,
                )
            }
        }

        // Progress segments
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            t.steps.indices.forEach { i ->
                val color = when {
                    i < stepIndex -> g.muted
                    i == stepIndex -> g.accent
                    else -> g.line2
                }
                Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(color).clickable { stepIndex = i })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                if (stepIndex < 0) "OVERVIEW" else "STEP ${"%02d".format(stepIndex + 1)} / ${t.steps.size}",
                style = Type.monoSmall, color = g.text2, modifier = Modifier.weight(1f),
            )
            Text("SAFETY · ${t.safety.label}", style = Type.monoSmall, color = safetyColor(t.safety))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (stepIndex < 0) Overview(t, nav) else {
                val step = t.steps[stepIndex]
                VSpace(8)
                Text(step.title, style = Type.title, color = g.text)
                VSpace(10)
                Text(step.body, style = Type.body, color = g.text2)
                step.caution?.let { VSpace(12); Note(it, g.accent) }
                if (step.substeps.isNotEmpty()) {
                    VSpace(12)
                    step.substeps.forEachIndexed { i, s ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("${i + 1}.", style = Type.mono, color = g.muted, modifier = Modifier.width(24.dp))
                            Text(s, style = Type.body, color = g.text2)
                        }
                    }
                }
                if (step.videos.isNotEmpty()) {
                    VSpace(14)
                    step.videos.forEach { v ->
                        Panel(Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = { context.openLink(v.url) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = g.text)
                                Spacer(Modifier.width(10.dp))
                                Text(v.label, style = Type.mono, color = g.text)
                            }
                        }
                    }
                }
                step.manualQuery?.let { q ->
                    VSpace(8)
                    RowItem("Look it up in the manual", "SEARCH: ${q.uppercase()}", onClick = { nav.manualSearch(q) })
                }
                if (step.checks.isNotEmpty()) {
                    VSpace(14)
                    SectionLabel("Check before next")
                    step.checks.forEachIndexed { i, label ->
                        val key = "$id:$stepIndex:$i"
                        val done = key in checks
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    scope.launch { if (done) c.db.settings().removeCheck(key) else c.db.settings().addCheck(StepCheck(key)) }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = done,
                                onCheckedChange = { now -> scope.launch { if (now) c.db.settings().addCheck(StepCheck(key)) else c.db.settings().removeCheck(key) } },
                                colors = CheckboxDefaults.colors(checkedColor = g.accent, uncheckedColor = g.line2),
                            )
                            Text(label, style = Type.body, color = if (done) g.muted else g.text)
                        }
                    }
                }
                VSpace(14)
                val existing = notes.firstOrNull { it.stepIndex == stepIndex }?.text ?: ""
                var note by remember(stepIndex, existing) { mutableStateOf(existing) }
                Field("Your notes for next time", note, {
                    note = it
                    scope.launch { c.db.settings().upsertNote(TutorialNote(id, stepIndex, it)) }
                }, singleLine = false)
                VSpace(24)
            }
        }

        // Bottom controls
        Column(Modifier.fillMaxWidth().background(g.bar).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Previous", { if (stepIndex > -1) stepIndex-- }, Modifier.weight(1f), enabled = stepIndex > -1, tall = true)
                if (stepIndex < t.steps.lastIndex) {
                    PrimaryButton(if (stepIndex < 0) "Start" else "Next step", { stepIndex++ }, Modifier.weight(2f), tall = true)
                } else {
                    PrimaryButton("Finish and log", {
                        nav.navigate("service/0?jobs=${t.jobIds.joinToString(",")}&tutorial=${t.id}")
                    }, Modifier.weight(2f), tall = true)
                }
            }
        }
    }
}

@Composable
private fun Overview(t: Tutorial, nav: NavHostController) {
    val g = LocalGarage.current
    val context = LocalContext.current
    VSpace(8)
    Text(t.title, style = Type.title, color = g.text)
    VSpace(10)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Tag("Difficulty ${t.difficulty}/5", g.text2)
        Tag("~${t.minutes} min", g.text2)
        Tag(t.safety.label, safetyColor(t.safety))
    }
    VSpace(12)
    Text(t.summary, style = Type.body, color = g.text2)
    if (t.tools.isNotEmpty()) {
        VSpace(16)
        SectionLabel("Tools")
        t.tools.forEach { Text("— $it", style = Type.body, color = g.text2, modifier = Modifier.padding(vertical = 2.dp)) }
    }
    if (t.parts.isNotEmpty()) {
        VSpace(16)
        SectionLabel("Parts")
        t.parts.forEach { Text("— $it", style = Type.body, color = g.text2, modifier = Modifier.padding(vertical = 2.dp)) }
    }
    if (t.references.isNotEmpty()) {
        VSpace(16)
        SectionLabel("Videos")
        t.references.forEach { v -> RowItem(v.label, "OPENS YOUTUBE", onClick = { context.openLink(v.url) }) }
    }
    VSpace(16)
    Note("Guidance only. Check specs against the official manual, and stop if something doesn't match what you see on the car.")
    VSpace(24)
}
