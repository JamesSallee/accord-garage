package app.accordgarage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import app.accordgarage.content.Urgency
import app.accordgarage.content.WarningLight
import app.accordgarage.content.WarningLights
import app.accordgarage.ui.components.Dot
import app.accordgarage.ui.components.EmptyState
import app.accordgarage.ui.components.GarageTopBar
import app.accordgarage.ui.components.LightGlyph
import app.accordgarage.ui.components.Note
import app.accordgarage.ui.components.PrimaryButton
import app.accordgarage.ui.components.SecondaryButton
import app.accordgarage.ui.components.SectionLabel
import app.accordgarage.ui.components.SelectPill
import app.accordgarage.ui.components.Tag
import app.accordgarage.ui.components.VSpace
import app.accordgarage.ui.manualSearch
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Type

@Composable
fun urgencyColor(u: Urgency): Color {
    val g = LocalGarage.current
    return when (u) {
        Urgency.STOP -> g.accent
        Urgency.CHECK -> g.amber
        Urgency.INFO -> g.muted
    }
}

// ---------------- Identifier ----------------

@Composable
fun WarningLightsScreen(nav: NavHostController) {
    val g = LocalGarage.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<Urgency?>(null) }
    val shown = WarningLights.all.filter { (filter == null || it.urgency == filter) && it.matches(query) }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Warning lights", "Tap the light you see on the dash", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe it: “key”, “red”, “blinking”", color = g.muted) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = g.muted) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = g.muted)
                    }
                },
                singleLine = true,
                textStyle = Type.body.copy(color = g.text),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = g.accent,
                    unfocusedBorderColor = g.line2,
                    cursorColor = g.accent,
                ),
            )
            VSpace(10)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectPill("All", filter == null, { filter = null })
                Urgency.entries.forEach { u -> SelectPill(u.label, filter == u, { filter = if (filter == u) null else u }) }
            }

            if (shown.isEmpty()) {
                EmptyState("No light matches that. Try a shape or a color, or search the manual.")
                SecondaryButton("Search the manual for “$query”", { nav.manualSearch(query.ifBlank { "indicator" }) }, Modifier.fillMaxWidth())
            }
            Urgency.entries.forEach { u ->
                val group = shown.filter { it.urgency == u }
                if (group.isNotEmpty()) {
                    VSpace(18)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(urgencyColor(u))
                        Spacer(Modifier.width(8.dp))
                        SectionLabel(u.label, trailing = "${group.size}")
                    }
                    VSpace(8)
                    group.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { light -> LightTile(light, Modifier.weight(1f)) { nav.navigate("light/${light.id}") } }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            VSpace(16)
            Text(
                "From the 2008 Accord Coupe owner's manual, for this car only: the U.S. EX-L 2.4L automatic. Symbols are redrawn; open a light's manual page to see Honda's own artwork.",
                style = Type.bodySmall,
                color = g.muted,
            )
            VSpace(32)
        }
    }
}

@Composable
private fun LightTile(light: WarningLight, modifier: Modifier, onClick: () -> Unit) {
    val g = LocalGarage.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(g.surface)
            .border(1.dp, g.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LightGlyph(light, 44.dp)
        VSpace(8)
        Text(
            light.name,
            style = Type.bodySmall.copy(fontSize = 12.sp),
            color = g.text2,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------- One light ----------------

@Composable
fun WarningLightScreen(nav: NavHostController, id: String) {
    val g = LocalGarage.current
    val light = WarningLights.byId(id)
    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Warning light", light?.name, onBack = { nav.popBackStack() })
        if (light == null) {
            EmptyState("That light isn't in the list.")
            return@Column
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            // The lamp as it sits in the cluster: lit symbol on a dark face.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (g.isDark) Color(0xFF050608) else Color(0xFF14181D))
                    .border(1.dp, g.line, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { LightGlyph(light, 84.dp) }
            VSpace(14)
            Text(light.name, style = Type.heading, color = g.text)
            VSpace(6)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(light.urgency.label, urgencyColor(light.urgency))
                Spacer(Modifier.width(10.dp))
                Text(light.looksLike, style = Type.bodySmall, color = g.muted)
            }

            Section("What it means") { Text(light.meaning, style = Type.body, color = g.text2) }
            light.normal?.let { n -> Section("When it's normal") { Text(n, style = Type.body, color = g.text2) } }
            Section("What to do") {
                light.steps.forEachIndexed { i, s ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        if (light.steps.size > 1) Text("${i + 1}", style = Type.mono, color = g.muted, modifier = Modifier.padding(end = 10.dp))
                        Text(s, style = Type.body, color = g.text2)
                    }
                }
            }
            light.caution?.let { VSpace(12); Note(it, g.accent) }

            VSpace(20)
            light.action?.let { (label, route) ->
                PrimaryButton(label, { nav.navigate(route) }, Modifier.fillMaxWidth())
                VSpace(10)
            }
            if (light.urgency == Urgency.STOP) {
                if (light.action == null) PrimaryButton("Roadside help", { nav.navigate("roadside") }, Modifier.fillMaxWidth())
                else SecondaryButton("Roadside help", { nav.navigate("roadside") }, Modifier.fillMaxWidth())
                VSpace(10)
            }
            SecondaryButton("See it in the manual", { nav.manualSearch(light.manualQuery) }, Modifier.fillMaxWidth())
            VSpace(10)
            Text("OWNER'S MANUAL ${light.pages.uppercase()}", style = Type.monoSmall, color = g.muted)
            VSpace(32)
        }
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    VSpace(18)
    SectionLabel(label)
    VSpace(6)
    content()
}
