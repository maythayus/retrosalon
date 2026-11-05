package fr.retrosalon.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme

/** Palette : ardoise bleutée, vert « manette » pour le focus. */
object Palette {
    val bg = Color(0xFF0F141A)
    val panel = Color(0xFF161D25)
    val raised = Color(0xFF1E2731)
    val line = Color(0xFF2A3440)
    val text = Color(0xFFE4E9EE)
    val muted = Color(0xFF93A0AD)
    val accent = Color(0xFF3FBF73)
    val onAccent = Color(0xFF06140B)
    val warn = Color(0xFFE8B75A)
    val bad = Color(0xFFF08A8A)
}

@Composable
fun RetroSalonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.accent,
            onPrimary = Palette.onAccent,
            background = Palette.bg,
            onBackground = Palette.text,
            surface = Palette.panel,
            onSurface = Palette.text,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides Palette.text) {
            Box(Modifier.background(Palette.bg)) { content() }
        }
    }
}

/** Élément cliquable à la télécommande ou à la manette : s'éclaire en vert au focus. */
@Composable
fun Tile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Palette.raised else Palette.panel,
            contentColor = Palette.text,
            focusedContainerColor = Palette.accent,
            focusedContentColor = Palette.onAccent,
            pressedContainerColor = Palette.accent,
            pressedContentColor = Palette.onAccent,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, if (selected) Palette.accent else Palette.line)),
            focusedBorder = Border(BorderStroke(2.dp, Palette.accent)),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), content = content)
    }
}

@Composable
fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    Tile(onClick = onClick, modifier = modifier, selected = primary) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = SurfaceDefaults.colors(containerColor = Palette.panel, contentColor = Palette.text),
        border = Border(BorderStroke(1.dp, Palette.line)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Palette.text)
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Palette.muted, modifier = modifier)
}

@Composable
fun Mono(text: String, color: Color = Palette.text) {
    Text(text, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp), color = color)
}

/** Petite étiquette d'état. */
@Composable
fun Badge(text: String, color: Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun ProgressBar(progress: Float?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(8.dp)
            .background(Palette.line, RoundedCornerShape(4.dp)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress ?: 0.08f)
                .background(Palette.accent, RoundedCornerShape(4.dp)),
        )
    }
}

/** Champ de saisie utilisable à la télécommande (le clavier de la TV s'ouvre au clic). */
@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Palette.muted)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Palette.text, fontSize = 18.sp),
            cursorBrush = SolidColor(Palette.accent),
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .background(Palette.raised, RoundedCornerShape(8.dp))
                .border(if (focused) 2.dp else 1.dp, if (focused) Palette.accent else Palette.line, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && hint.isNotEmpty()) Text(hint, color = Palette.muted.copy(alpha = 0.6f), fontSize = 18.sp)
                    inner()
                }
            },
        )
    }
}

@Composable
fun ButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}
