package com.lexdiary.app

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val Ink = Color(0xFF23374D)
private val Blue = Color(0xFF4D8CCB)
private val PaleBlue = Color(0xFFEAF4FC)
private val Paper = Color(0xFFF8FBFE)
private val Palette = listOf(
    Color(0xFF23374D), Color(0xFF4D8CCB), Color(0xFFDA6871),
    Color(0xFF63A47B), Color(0xFFE2A54F), Color.White
)

private data class PointData(val x: Float, val y: Float)
private data class StrokeData(val points: List<PointData>, val color: Int, val width: Float = 5f)
private data class TextData(val text: String, val x: Float, val y: Float, val color: Int)
private data class PhotoData(val path: String, val x: Float = 0.05f, val y: Float = 0.12f)
private data class JournalEntry(
    val strokes: List<StrokeData> = emptyList(),
    val texts: List<TextData> = emptyList(),
    val photos: List<PhotoData> = emptyList()
)
private data class PlanItem(val id: String, val text: String)
private data class Profile(val name: String = "Lex", val avatarPath: String = "")
private enum class Page { HOME, ME, EDIT, HISTORY, FUTURE }
private enum class EditorTool { NONE, DRAW }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(248, 251, 254)
        window.navigationBarColor = android.graphics.Color.rgb(248, 251, 254)
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { DiaryApp() }
    }
}

private class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("today_journal", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun entries(): Map<String, JournalEntry> = decode("entries", object : TypeToken<Map<String, JournalEntry>>() {}.type) ?: emptyMap()
    fun plans(): Map<String, List<PlanItem>> = decode("plans", object : TypeToken<Map<String, List<PlanItem>>>() {}.type) ?: emptyMap()
    fun profile(): Profile = decode("profile", Profile::class.java) ?: Profile()

    fun saveEntries(value: Map<String, JournalEntry>) = prefs.edit().putString("entries", gson.toJson(value)).apply()
    fun savePlans(value: Map<String, List<PlanItem>>) = prefs.edit().putString("plans", gson.toJson(value)).apply()
    fun saveProfile(value: Profile) = prefs.edit().putString("profile", gson.toJson(value)).apply()

    private fun <T> decode(key: String, type: java.lang.reflect.Type): T? =
        prefs.getString(key, null)?.let { runCatching { gson.fromJson<T>(it, type) }.getOrNull() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryApp() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    val entries = remember { mutableStateMapOf<String, JournalEntry>().apply { putAll(store.entries()) } }
    val plans = remember { mutableStateMapOf<String, List<PlanItem>>().apply { putAll(store.plans()) } }
    var profile by remember { mutableStateOf(store.profile()) }
    var page by remember { mutableStateOf(Page.HOME) }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    val today = LocalDate.now().toString()

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Blue,
            onPrimary = Color.White,
            background = Paper,
            surface = Color.White,
            onSurface = Ink
        )
    ) {
        Scaffold(
            containerColor = Paper,
            bottomBar = {
                if (page == Page.HOME || page == Page.ME) {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(
                            selected = page == Page.HOME,
                            onClick = { page = Page.HOME },
                            icon = { Text("⌂", fontSize = 22.sp) },
                            label = { Text("主页") }
                        )
                        NavigationBarItem(
                            selected = page == Page.ME,
                            onClick = { page = Page.ME },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { Text("我的") }
                        )
                    }
                }
            }
        ) { padding ->
            when (page) {
                Page.HOME -> HomePage(
                    modifier = Modifier.padding(padding),
                    name = profile.name,
                    onToday = { selectedDate = today; page = Page.EDIT },
                    onHistory = { page = Page.HISTORY },
                    onFuture = { selectedDate = LocalDate.now().plusDays(1).toString(); page = Page.FUTURE }
                )
                Page.ME -> ProfilePage(
                    modifier = Modifier.padding(padding),
                    profile = profile,
                    onProfileChange = { profile = it; store.saveProfile(it) }
                )
                Page.EDIT -> EditorPage(
                    date = selectedDate,
                    entry = entries[selectedDate] ?: JournalEntry(),
                    plans = plans[selectedDate].orEmpty(),
                    onBack = { page = Page.HOME },
                    onSave = { entries[selectedDate] = it; store.saveEntries(entries.toMap()) },
                    onToast = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
                Page.HISTORY -> HistoryPage(
                    entries = entries.toMap(),
                    today = today,
                    onBack = { page = Page.HOME },
                    onOpen = { selectedDate = it; page = Page.EDIT }
                )
                Page.FUTURE -> FuturePage(
                    date = selectedDate,
                    plans = plans[selectedDate].orEmpty(),
                    onDateChange = { selectedDate = it },
                    onBack = { page = Page.HOME },
                    onPlansChange = { plans[selectedDate] = it; store.savePlans(plans.toMap()) }
                )
            }
        }
    }
}

@Composable
private fun HomePage(
    modifier: Modifier,
    name: String,
    onToday: () -> Unit,
    onHistory: () -> Unit,
    onFuture: () -> Unit
) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("今日手记", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("${LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 · E", Locale.CHINA))}  ·  $name", color = Blue, fontSize = 14.sp)
            }
            Box(Modifier.size(42.dp).clip(CircleShape).background(PaleBlue), contentAlignment = Alignment.Center) {
                Text("✦", color = Blue, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).padding(18.dp)
                .clip(RoundedCornerShape(32.dp)).background(PaleBlue),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(LocalDate.now().dayOfMonth.toString(), fontSize = 94.sp, fontWeight = FontWeight.Light, color = Blue)
                Text("把今天，留给自己", fontSize = 16.sp, color = Ink)
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onToday,
                    modifier = Modifier.fillMaxWidth(0.78f).height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) { Text("记录今天", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeAction("回顾昨日", "翻一翻旧时光", Icons.AutoMirrored.Filled.ArrowBack, onHistory, Modifier.weight(1f))
            HomeAction("畅想未来", "写下下一步", Icons.AutoMirrored.Filled.ArrowForward, onFuture, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun HomeAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Card(
        modifier = modifier.height(96.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Blue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = Color(0xFF75869A), fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorPage(
    date: String,
    entry: JournalEntry,
    plans: List<PlanItem>,
    onBack: () -> Unit,
    onSave: (JournalEntry) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val isToday = date == LocalDate.now().toString()
    val strokes = remember(date) { mutableStateListOf<StrokeData>().apply { addAll(entry.strokes) } }
    val texts = remember(date) { mutableStateListOf<TextData>().apply { addAll(entry.texts) } }
    val photos = remember(date) { mutableStateListOf<PhotoData>().apply { addAll(entry.photos) } }
    var selectedColor by remember { mutableStateOf(Palette[0]) }
    var tool by remember { mutableStateOf(EditorTool.NONE) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showEmojiDialog by remember { mutableStateOf(false) }
    var editingTextIndex by remember { mutableStateOf<Int?>(null) }
    var textDraft by remember { mutableStateOf("") }
    var previewPath by remember { mutableStateOf<String?>(null) }
    var currentStroke by remember { mutableStateOf<List<PointData>>(emptyList()) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri -> copyImageToPrivateStorage(context, uri)?.let { photos.add(PhotoData(it)) } }
    }
    val latestEntry = JournalEntry(strokes.toList(), texts.toList(), photos.toList())
    LaunchedEffect(latestEntry, date) { onSave(latestEntry) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TopAppBar(
            title = { Column { Text(if (isToday) "记录今天" else "日记详情", fontWeight = FontWeight.SemiBold); Text(prettyDate(date), fontSize = 12.sp, color = Blue) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } }
        )
        if (plans.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = PaleBlue), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("給今天的自己", color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    plans.forEach { Text("• ${it.text}", color = Ink, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (isToday) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EditorIcon(Icons.Default.TextFields, "文字") { tool = EditorTool.NONE; editingTextIndex = null; textDraft = ""; showTextDialog = true }
                    EditorIcon(Icons.Default.Brush, if (tool == EditorTool.DRAW) "停止手绘" else "手绘", selected = tool == EditorTool.DRAW) { tool = if (tool == EditorTool.DRAW) EditorTool.NONE else EditorTool.DRAW }
                    EditorIcon(null, "表情") { showEmojiDialog = true }
                    EditorIcon(Icons.Default.Image, "添加图片") { imagePicker.launch("image/*") }
                    EditorIcon(Icons.Default.Undo, "撤销") {
                        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
                        else if (texts.isNotEmpty()) texts.removeAt(texts.lastIndex)
                        else if (photos.isNotEmpty()) photos.removeAt(photos.lastIndex)
                    }
                    EditorIcon(Icons.Default.DeleteOutline, "清空") { strokes.clear(); texts.clear(); photos.clear() }
                }
            }
        }
        if (isToday) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("顏色", color = Color(0xFF75869A), fontSize = 12.sp)
                Palette.forEach { color ->
                    Box(
                        Modifier.size(25.dp).clip(CircleShape).background(color)
                            .border(if (selectedColor == color) 2.dp else 1.dp, if (selectedColor == color) Blue else Color(0xFFD7E2EC), CircleShape)
                            .clickable { selectedColor = color }
                    )
                }
                Spacer(Modifier.weight(1f))
                if (tool == EditorTool.DRAW) Text("在画布上滑动手绘", color = Blue, fontSize = 11.sp)
            }
        }
        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).padding(bottom = 8.dp)
                .clip(RoundedCornerShape(18.dp)).background(Color.White)
                .border(1.dp, Color(0xFFE2ECF4), RoundedCornerShape(18.dp))
        ) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight
            Canvas(
                Modifier.fillMaxSize().pointerInput(isToday, tool, selectedColor, photos.toList()) {
                    if (isToday && tool == EditorTool.DRAW) {
                        detectDragGestures(
                            onDragStart = { position -> currentStroke = listOf(PointData(position.x / size.width, position.y / size.height)) },
                            onDrag = { change, _ ->
                                currentStroke = currentStroke + PointData(change.position.x / size.width, change.position.y / size.height)
                            },
                            onDragEnd = {
                                if (currentStroke.size > 1) strokes.add(StrokeData(currentStroke, colorToArgb(selectedColor)))
                                currentStroke = emptyList()
                            }
                        )
                    } else if (!isToday) {
                        detectTapGestures { position ->
                            val photo = photos.lastOrNull {
                                position.x / size.width in it.x..(it.x + 0.88f) &&
                                    position.y / size.height in it.y..(it.y + 0.62f)
                            }
                            if (photo == null) onToast("请更加关注当下吧")
                        }
                    }
                }
            ) {}
            photos.forEach { photo ->
                val bitmap = remember(photo.path) { runCatching { BitmapFactory.decodeFile(photo.path)?.asImageBitmap() }.getOrNull() }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "日记照片",
                        modifier = Modifier.offset { IntOffset((canvasWidth * photo.x).roundToPx(), (canvasHeight * photo.y).roundToPx()) }
                            .fillMaxWidth(0.88f).height(canvasHeight * 0.62f)
                            .clip(RoundedCornerShape(10.dp))
                            .then(if (!isToday) Modifier.clickable { previewPath = photo.path } else Modifier),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                (strokes + listOfNotNull(if (currentStroke.isNotEmpty()) StrokeData(currentStroke, colorToArgb(selectedColor)) else null)).forEach { stroke ->
                    val path = Path()
                    stroke.points.forEachIndexed { index, point ->
                        val position = Offset(point.x * size.width, point.y * size.height)
                        if (index == 0) path.moveTo(position.x, position.y) else path.lineTo(position.x, position.y)
                    }
                    drawPath(path, Color(stroke.color), style = Stroke(width = stroke.width))
                }
            }
            texts.forEachIndexed { index, text ->
                var dragX by remember(date, index) { mutableStateOf(text.x) }
                var dragY by remember(date, index) { mutableStateOf(text.y) }
                Text(
                    text = text.text,
                    color = Color(text.color),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.offset { IntOffset((canvasWidth * dragX).roundToPx(), (canvasHeight * dragY).roundToPx()) }
                        .then(
                            if (isToday) Modifier.pointerInput(index, canvasWidth, canvasHeight) {
                                detectDragGestures { change, amount ->
                                    change.consume()
                                    dragX = (dragX + amount.x / size.width).coerceIn(0f, 0.85f)
                                    dragY = (dragY + amount.y / size.height).coerceIn(0f, 0.9f)
                                    texts[index] = texts[index].copy(x = dragX, y = dragY)
                                }
                            } else Modifier.clickable { onToast("请更加关注当下吧") }
                        )
                )
            }
            if (isToday && entry == JournalEntry() && strokes.isEmpty() && texts.isEmpty() && photos.isEmpty()) {
                Text("輕觸工具開始記錄", color = Color(0xFF9AAABA), modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showTextDialog || showEmojiDialog) {
        val emojiMode = showEmojiDialog
        AlertDialog(
            onDismissRequest = { showTextDialog = false; showEmojiDialog = false },
            title = { Text(if (emojiMode) "選擇表情" else "添加文字") },
            text = {
                if (emojiMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("😊", "🌿", "☀️", "💙", "✨", "🌙").forEach { emoji ->
                            Text(emoji, fontSize = 25.sp, modifier = Modifier.clickable { textDraft += emoji })
                        }
                    }
                } else {
                    Column {
                        OutlinedTextField(textDraft, { textDraft = it }, label = { Text("寫下此刻") }, minLines = 2)
                        ColorPicker(selectedColor) { selectedColor = it }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textDraft.isNotBlank()) {
                        val oldIndex = editingTextIndex
                        if (oldIndex != null && oldIndex in texts.indices) texts[oldIndex] = texts[oldIndex].copy(text = textDraft, color = colorToArgb(selectedColor))
                        else texts.add(TextData(textDraft, 0.12f, 0.12f, colorToArgb(selectedColor)))
                    }
                    textDraft = ""; editingTextIndex = null; showTextDialog = false; showEmojiDialog = false
                }) { Text("放到画布上") }
            },
            dismissButton = { TextButton(onClick = { textDraft = ""; showTextDialog = false; showEmojiDialog = false }) { Text("取消") } }
        )
    }
    previewPath?.let { path ->
        Dialog(onDismissRequest = { previewPath = null }) {
            val bitmap = remember(path) { runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
            if (bitmap != null) Image(bitmap, "放大的日记照片", Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(12.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
        }
    }
}

@Composable
private fun EditorIcon(icon: androidx.compose.ui.graphics.vector.ImageVector?, label: String, selected: Boolean = false, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(39.dp).background(if (selected) PaleBlue else Color.Transparent, CircleShape)) {
        if (icon != null) Icon(icon, contentDescription = label, tint = if (selected) Blue else Ink, modifier = Modifier.size(19.dp))
        else Text("☺", color = Ink, fontSize = 19.sp)
    }
}

@Composable
private fun ColorPicker(selected: Color, onSelect: (Color) -> Unit) {
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Palette.forEach { color ->
            Box(Modifier.size(25.dp).clip(CircleShape).background(color).border(if (selected == color) 2.dp else 1.dp, Blue, CircleShape).clickable { onSelect(color) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryPage(entries: Map<String, JournalEntry>, today: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val dates = entries.keys.filter { it < today }.sortedDescending()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TopAppBar(title = { Text("回顾昨日", fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } })
        if (dates.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("还没有更早的日记", color = Color(0xFF75869A)) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
                items(dates) { date ->
                    val entry = entries[date] ?: JournalEntry()
                    Card(Modifier.fillMaxWidth().clickable { onOpen(date) }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(prettyDate(date), color = Blue, fontWeight = FontWeight.SemiBold)
                            entry.texts.firstOrNull()?.let { Text(it.text, color = Ink, maxLines = 2, modifier = Modifier.padding(top = 6.dp)) }
                            if (entry.photos.isNotEmpty()) Text("📷 ${entry.photos.size} 张图片", color = Color(0xFF75869A), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            if (entry.texts.isEmpty() && entry.photos.isEmpty() && entry.strokes.isEmpty()) Text("一页留白", color = Color(0xFF9AAABA), modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuturePage(date: String, plans: List<PlanItem>, onDateChange: (String) -> Unit, onBack: () -> Unit, onPlansChange: (List<PlanItem>) -> Unit) {
    var draft by remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TopAppBar(title = { Text("畅想未来", fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } })
        OutlinedButton(
            onClick = {
                val parsed = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now().plusDays(1))
                DatePickerDialog(context, { _, year, month, day -> onDateChange(LocalDate.of(year, month + 1, day).toString()) }, parsed.year, parsed.monthValue - 1, parsed.dayOfMonth).show()
            },
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(prettyDate(date))
        }
        Text("写给那一天的自己", color = Color(0xFF75869A), fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(draft, { draft = it }, modifier = Modifier.weight(1f), placeholder = { Text("一个小小的计划…") }, singleLine = true)
            IconButton(onClick = {
                if (draft.isNotBlank()) {
                    onPlansChange(plans + PlanItem(System.currentTimeMillis().toString(), draft.trim()))
                    draft = ""
                }
            }) { Icon(Icons.Default.Add, contentDescription = "添加计划", tint = Blue) }
        }
        Spacer(Modifier.height(16.dp))
        plans.forEach { plan ->
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("☼", color = Blue, fontSize = 20.sp)
                    Text(plan.text, Modifier.weight(1f).padding(horizontal = 12.dp), color = Ink)
                    IconButton(onClick = { onPlansChange(plans.filterNot { it.id == plan.id }) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "删除计划", tint = Color(0xFF8796A7)) }
                }
            }
        }
    }
}

@Composable
private fun ProfilePage(modifier: Modifier, profile: Profile, onProfileChange: (Profile) -> Unit) {
    val context = LocalContext.current
    var showNameDialog by remember { mutableStateOf(false) }
    var nameDraft by remember(profile.name) { mutableStateOf(profile.name) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { copyImageToPrivateStorage(context, it)?.let { path -> onProfileChange(profile.copy(avatarPath = path)) } }
    }
    Column(modifier.fillMaxSize().padding(24.dp)) {
        Text("我的", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Avatar(profile.avatarPath, Modifier.size(68.dp).clickable { imagePicker.launch("image/*") })
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(profile.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("点头像可以更换", color = Color(0xFF75869A), fontSize = 13.sp)
            }
            IconButton(onClick = { nameDraft = profile.name; showNameDialog = true }) { Icon(Icons.Default.Edit, contentDescription = "修改用户名", tint = Blue) }
        }
        Spacer(Modifier.height(28.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("只存在这台手机", fontWeight = FontWeight.SemiBold, color = Ink)
                Text("日记不会上传到云端。卸载应用或清除数据后，内容将无法恢复。", color = Color(0xFF75869A), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("修改用户名") },
            text = { OutlinedTextField(nameDraft, { nameDraft = it }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)) },
            confirmButton = { TextButton(onClick = { if (nameDraft.isNotBlank()) onProfileChange(profile.copy(name = nameDraft.trim())); showNameDialog = false }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun Avatar(path: String, modifier: Modifier = Modifier) {
    val bitmap = remember(path) { if (path.isNotBlank()) runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() else null }
    if (bitmap != null) Image(bitmap, "头像", modifier.clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    else Box(modifier.clip(CircleShape).background(PaleBlue), contentAlignment = Alignment.Center) { Text("✦", color = Blue, fontSize = 27.sp) }
}

private fun copyImageToPrivateStorage(context: Context, uri: Uri): String? = runCatching {
    val destination = File(context.filesDir, "image_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri).use { input -> destination.outputStream().use { output -> input?.copyTo(output) } }
    destination.absolutePath
}.getOrNull()

private fun prettyDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("yyyy年M月d日 · E", Locale.CHINA))
}.getOrDefault(value)

private fun colorToArgb(color: Color): Int = android.graphics.Color.argb(
    (color.alpha * 255).roundToInt(),
    (color.red * 255).roundToInt(),
    (color.green * 255).roundToInt(),
    (color.blue * 255).roundToInt()
)
