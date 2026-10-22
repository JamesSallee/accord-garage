@file:OptIn(ExperimentalMaterial3Api::class)

package app.accordgarage.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.accordgarage.domain.DueState
import app.accordgarage.ui.theme.Inter
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Mono
import app.accordgarage.ui.theme.Type
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- formatting ----------

object Fmt {
    private val miles = NumberFormat.getIntegerInstance(Locale.US)
    private val money = NumberFormat.getCurrencyInstance(Locale.US)
    private val date = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

    fun miles(v: Int): String = miles.format(v)
    fun money(v: Double): String = money.format(v)
    fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(date)
    fun date(d: LocalDate): String = d.format(date)
    fun num(v: Double, decimals: Int = 1): String = String.format(Locale.US, "%.${decimals}f", v)
}

/** Parses a number typed by the owner; tolerates commas and a $ sign. */
fun String.toNumberOrNull(): Double? = replace(",", "").replace("$", "").trim().toDoubleOrNull()

@Composable
fun stateColor(state: DueState): Color {
    val g = LocalGarage.current
    return when (state) {
        DueState.OVERDUE -> g.accent
        DueState.MINDER, DueState.CATCH_UP, DueState.DUE_SOON -> g.amber
        DueState.OK -> g.green
        else -> g.muted
    }
}

// ---------- building blocks ----------

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val g = LocalGarage.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = Type.label, color = g.muted, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = Type.monoSmall, color = g.muted)
    }
}

@Composable
fun Dot(color: Color, size: Dp = 7.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun GarageTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val g = LocalGarage.current
    Row(
        Modifier.fillMaxWidth().padding(start = if (onBack != null) 4.dp else 20.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = g.text2)
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = Type.label, color = g.muted)
            if (subtitle != null) Text(subtitle, style = Type.bodySmall, color = g.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val g = LocalGarage.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .clip(shape)
            .background(g.surface)
            .border(1.dp, g.line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
    ) { content() }
}

@Composable
fun Tile(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    dot: Color? = null,
    valueColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val g = LocalGarage.current
    Panel(modifier, onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dot != null) {
                    Dot(dot, 6.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(label.uppercase(), style = Type.label.copy(fontSize = 10.5.sp, letterSpacing = 1.4.sp), color = g.muted, maxLines = 1)
            }
            Spacer(Modifier.size(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = Type.tileValue, color = valueColor ?: g.text, maxLines = 1)
                if (unit != null) {
                    Spacer(Modifier.width(3.dp))
                    Text(unit, style = Type.mono.copy(fontSize = 13.sp), color = g.muted, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
            Spacer(Modifier.size(4.dp))
            Text(sub, style = Type.bodySmall.copy(fontSize = 11.5.sp), color = g.muted, maxLines = 2)
        }
    }
}

@Composable
fun RowItem(
    title: String,
    sub: String? = null,
    onClick: (() -> Unit)? = null,
    dot: Color? = null,
    trailing: String? = null,
    trailingColor: Color? = null,
    icon: ImageVector? = null,
    showChevron: Boolean = onClick != null,
) {
    val g = LocalGarage.current
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dot != null) {
                Dot(dot)
                Spacer(Modifier.width(12.dp))
            }
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = g.text2, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = Type.rowTitle, color = g.text)
                if (!sub.isNullOrBlank()) Text(sub, style = Type.monoSmall, color = g.muted)
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Text(trailing, style = Type.mono, color = trailingColor ?: g.text2)
            }
            if (showChevron) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = g.line2)
            }
        }
        HorizontalDivider(color = g.surface2, thickness = 1.dp)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, tall: Boolean = false) {
    val g = LocalGarage.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = if (tall) 56.dp else 48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = g.accentFill, contentColor = g.onAccentFill, disabledContainerColor = g.surface2, disabledContentColor = g.muted),
    ) { Text(text, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, tall: Boolean = false, color: Color? = null) {
    val g = LocalGarage.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = if (tall) 56.dp else 48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, color?.copy(alpha = 0.6f) ?: g.line2),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color ?: g.text),
    ) { Text(text, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp) }
}

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    number: Boolean = false,
    mono: Boolean = false,
    singleLine: Boolean = true,
    placeholder: String? = null,
) {
    val g = LocalGarage.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { if (placeholder != null) Text(placeholder, color = g.muted) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        textStyle = if (mono) Type.mono.copy(fontSize = 15.sp, color = g.text) else Type.body.copy(color = g.text),
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = g.accent,
            unfocusedBorderColor = g.line2,
            focusedLabelColor = g.accent,
            unfocusedLabelColor = g.muted,
            cursorColor = g.accent,
        ),
    )
}

@Composable
fun DateField(label: String, epochDay: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    val g = LocalGarage.current
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .border(1.dp, g.line2, shape)
            .clickable { open = true }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Type.bodySmall.copy(fontSize = 12.sp), color = g.muted)
            Text(Fmt.date(epochDay), style = Type.mono.copy(fontSize = 15.sp), color = g.text)
        }
        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick date", tint = g.muted)
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
fun SelectPill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val g = LocalGarage.current
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (selected) g.accent.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (selected) g.accent else g.line2, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.bodySmall.copy(fontSize = 14.sp), color = if (selected) g.text else g.text2)
    }
}

@Composable
fun Tag(text: String, color: Color) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .clip(shape)
            .border(1.dp, color.copy(alpha = 0.5f), shape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) { Text(text.uppercase(), style = Type.monoSmall.copy(fontSize = 10.sp), color = color) }
}

@Composable
fun Note(text: String, color: Color? = null) {
    val g = LocalGarage.current
    val c = color ?: g.muted
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.copy(alpha = 0.08f))
            .border(1.dp, c.copy(alpha = 0.35f), shape)
            .padding(12.dp),
    ) { Text(text, style = Type.bodySmall, color = g.text2) }
}

@Composable
fun EmptyState(text: String) {
    val g = LocalGarage.current
    Text(text, style = Type.body, color = g.muted, modifier = Modifier.padding(vertical = 24.dp))
}

@Composable
fun MonoValue(text: String, color: Color? = null, size: Int = 15) {
    Text(text, fontFamily = Mono, fontSize = size.sp, color = color ?: LocalGarage.current.text)
}

val ScreenPadding = Modifier.padding(horizontal = 20.dp)

@Composable
fun VSpace(h: Int) = Spacer(Modifier.size(h.dp))

@Composable
fun ButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}
