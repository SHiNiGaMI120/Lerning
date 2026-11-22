package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LingoBlue = Color(0xFF5556E8)
val LingoViolet = Color(0xFF8F65F4)
val LingoPeach = Color(0xFFFFBC83)
val LingoMint = Color(0xFFBCF4DE)
val LingoInk = Color(0xFF191B38)

/** Original vector companion. No remote images or loading are needed. */
@Composable
fun LumoCompanion(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(Color.White.copy(alpha = .07f), w * .49f, Offset(w * .5f, h * .48f))
        drawCircle(Color.White.copy(alpha = .13f), w * .39f, Offset(w * .5f, h * .48f), style = Stroke(w * .012f))
        drawOval(Color(0xFF1E1A70).copy(alpha = .22f), Offset(w * .2f, h * .83f), Size(w * .62f, h * .09f))
        drawLine(LingoPeach, Offset(w * .51f, h * .17f), Offset(w * .51f, h * .28f), w * .035f, StrokeCap.Round)
        drawCircle(LingoPeach, w * .055f, Offset(w * .51f, h * .14f))
        drawRoundRect(Color(0xFFDADDFE), Offset(w * .17f, h * .34f), Size(w * .69f, h * .48f), CornerRadius(w * .19f))
        drawRoundRect(Color.White, Offset(w * .14f, h * .29f), Size(w * .69f, h * .48f), CornerRadius(w * .19f))
        drawRoundRect(LingoInk, Offset(w * .24f, h * .4f), Size(w * .49f, h * .24f), CornerRadius(w * .1f))
        drawRoundRect(LingoMint, Offset(w * .33f, h * .47f), Size(w * .055f, h * .08f), CornerRadius(w * .025f))
        drawRoundRect(LingoMint, Offset(w * .59f, h * .47f), Size(w * .055f, h * .08f), CornerRadius(w * .025f))
        val smile = Path().apply { moveTo(w * .43f, h * .55f); quadraticTo(w * .49f, h * .61f, w * .55f, h * .55f) }
        drawPath(smile, Color.White, style = Stroke(w * .016f, cap = StrokeCap.Round))
        drawRoundRect(LingoPeach, Offset(w * .07f, h * .43f), Size(w * .1f, h * .19f), CornerRadius(w * .045f))
        drawRoundRect(LingoPeach, Offset(w * .8f, h * .43f), Size(w * .1f, h * .19f), CornerRadius(w * .045f))
        drawCircle(Color.White.copy(alpha = .8f), w * .024f, Offset(w * .08f, h * .21f))
        drawCircle(LingoMint, w * .03f, Offset(w * .92f, h * .76f))
    }
}

@Composable
fun StudioPill(text: String, color: Color = MaterialTheme.colorScheme.primary, onColor: Color = color) {
    Surface(shape = CircleShape, color = color.copy(alpha = .12f)) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = onColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StudioSectionTitle(title: String, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.width(4.dp).height(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun StudioFeatureCard(
    title: String, subtitle: String, icon: ImageVector, accent: Color,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = .16f)) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(12.dp).size(24.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StudioVoiceBanner(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(
        Brush.linearGradient(listOf(Color(0xFF29264F), Color(0xFF47417B), Color(0xFF62559A))))) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مساحة من غير إحراج", color = LingoPeach, style = MaterialTheme.typography.labelMedium)
                Text("صوتك يفتح\nألف حكاية.", color = Color.White, fontSize = 27.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold)
                Text("مدرس ذكي. تمرين على طريقتك.", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall)
            }
            LumoCompanion(Modifier.size(108.dp))
        }
    }
}

@Composable
fun SentenceRevealCard(target: String, translation: String, cardKey: String) {
    var revealed by androidx.compose.runtime.remember(cardKey) { androidx.compose.runtime.mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StudioPill("اسمعها. تخيّلها. قولها.")
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                Text(target, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = com.example.ui.theme.LatinFontFamily),
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth())
            }
            androidx.compose.animation.AnimatedVisibility(revealed) {
                Text(translation, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            TextButton(onClick = { revealed = !revealed }, modifier = Modifier.fillMaxWidth().testTag("sentence_reveal_translation")) {
                Text(if (revealed) "اخفي المعنى وجرّب تفتكره" else "اضغط واكتشف المعنى ↗", fontWeight = FontWeight.Bold)
            }
        }
    }
}
