package com.example.workouttracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// --- МОДЕЛИ ДАННЫХ ---

data class Exercise(
    val id: String,
    val name: String,
    val equipment: String, // "Пол", "Стена", "Стул/Диван", "Турник/Дверь"
    val strengthScore: Int,    // 1-10
    val functionalScore: Int,  // 1-10
    val cardioScore: Int,      // 1-10
    val coreScore: Int         // 1-10
)

data class WorkoutTemplate(
    val id: String,
    val title: String,
    val focusDescription: String,
    val targetCategory: String,
    val defaultSets: Int,
    val defaultReps: String,
    val exercises: List<Exercise>
)

data class SessionHistory(
    val id: Long,
    val date: String,
    val programTitle: String,
    val adaptationMode: String,
    val completedExercises: String,
    val energyRating: Int, // 1-5
    val rpeRating: Int,    // 1-10
    val note: String
)

// --- БАЗА УПРАЖНЕНИЙ С МНОГОМЕРНЫМИ БАЛЛАМИ (1-10) ---

val CalisthenicsExerciseDb = listOf(
    Exercise("p_push", "Алмазные / Узкие отжимания", "Пол", 8, 6, 4, 7),
    Exercise("p_dips", "Отжимания от стула (брусья-стиль)", "Стул/Диван", 8, 7, 4, 6),
    Exercise("p_pike", "Пайк-отжимания (на плечи)", "Пол", 9, 8, 3, 7),
    Exercise("p_pull", "Подтягивания / Тяга к столу", "Турник/Стол", 10, 8, 3, 8),
    Exercise("l_pistol", "Приседания на одной ноге (ассист)", "Стул/Стена", 9, 9, 5, 8),
    Exercise("l_bulg", "Болгарские сплит-приседы", "Стул/Диван", 8, 8, 6, 6),
    Exercise("l_jump", "Взрывные выпрыгивания из седа", "Пол", 6, 8, 9, 6),
    Exercise("c_burp", "Бёрпи с чистым упором", "Пол", 5, 8, 10, 7),
    Exercise("c_climb", "Скалолаз (Mountain Climbers)", "Пол", 4, 7, 9, 8),
    Exercise("k_hollow", "Hollow Body Hold (Лодочка)", "Пол", 7, 10, 2, 10),
    Exercise("k_plank", "Динамическая планка на предплечьях", "Пол", 5, 8, 4, 9),
    Exercise("k_bridge", "Ягодичный мостик на одной ноге", "Пол", 7, 8, 3, 8)
)

val BuiltInTemplates = listOf(
    WorkoutTemplate(
        id = "t_strength",
        title = "Силовой базис (Overcoming Gravity)",
        focusDescription = "Максимальный рекрутинг волокон и сила без железа. Акцент на сложные прогрессии.",
        targetCategory = "Сила",
        defaultSets = 4,
        defaultReps = "6-8",
        exercises = listOf(
            CalisthenicsExerciseDb[2], // Пайк
            CalisthenicsExerciseDb[3], // Тяга/Подтягивания
            CalisthenicsExerciseDb[0], // Алмазные отжимания
            CalisthenicsExerciseDb[5], // Болгарские сплиты
            CalisthenicsExerciseDb[9]  // Hollow body
        )
    ),
    WorkoutTemplate(
        id = "t_functional",
        title = "Функциональный атлетизм (Координация + Кор)",
        focusDescription = "Баланс тела в пространстве, здоровье связок, взрывная сила и глубокий кор.",
        targetCategory = "Функционал",
        defaultSets = 3,
        defaultReps = "10-12",
        exercises = listOf(
            CalisthenicsExerciseDb[4], // Пистолетики
            CalisthenicsExerciseDb[1], // Отжимания от стула
            CalisthenicsExerciseDb[11], // Ягодичный мост
            CalisthenicsExerciseDb[10], // Динамическая планка
            CalisthenicsExerciseDb[6]  // Взрывные выпрыгивания
        )
    ),
    WorkoutTemplate(
        id = "t_cardio",
        title = "Метаболический кондишн (HIIT/Жиросжигание)",
        focusDescription = "Высокая ЧСС, плотность подходов, кислородный долг без ударного вреда для суставов.",
        targetCategory = "Кардио",
        defaultSets = 4,
        defaultReps = "15-20",
        exercises = listOf(
            CalisthenicsExerciseDb[7], // Берпи
            CalisthenicsExerciseDb[8], // Скалолаз
            CalisthenicsExerciseDb[6], // Выпрыгивания
            CalisthenicsExerciseDb[0], // Отжимания
            CalisthenicsExerciseDb[10] // Планка
        )
    )
)

// --- ГЛАВНАЯ АКТИВНОСТЬ ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkoutTrackerTheme {
                MainScreen(context = this)
            }
        }
    }
}

@Composable
fun WorkoutTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF121418),
            surface = Color(0xFF1B1E24),
            surfaceVariant = Color(0xFF262A34),
            primary = Color(0xFF4E80EE),
            secondary = Color(0xFF2EC4B6),
            tertiary = Color(0xFFFF9F1C),
            onBackground = Color(0xFFEAEAEA),
            onSurface = Color(0xFFF1F1F1)
        ),
        content = content
    )
}

// --- НАВИГАЦИЯ И ГЛАВНЫЙ ЭКРАН ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(context: Context) {
    val prefs = remember { context.getSharedPreferences("calisthenics_v2", Context.MODE_PRIVATE) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Шаблоны, 1: Конструктор/База, 2: Журнал
    var activeTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    val historyList = remember { mutableStateListOf<SessionHistory>() }

    // Загрузка истории
    LaunchedEffect(Unit) {
        val rawJson = prefs.getString("history", "[]") ?: "[]"
        try {
            val arr = JSONArray(rawJson)
            historyList.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                historyList.add(
                    SessionHistory(
                        id = obj.getLong("id"),
                        date = obj.getString("date"),
                        programTitle = obj.getString("programTitle"),
                        adaptationMode = obj.getString("adaptationMode"),
                        completedExercises = obj.getString("completedExercises"),
                        energyRating = obj.getInt("energyRating"),
                        rpeRating = obj.getInt("rpeRating"),
                        note = obj.getString("note")
                    )
                )
            }
        } catch (_: Exception) {}
    }

    fun saveHistory() {
        val arr = JSONArray()
        historyList.forEach {
            val obj = JSONObject().apply {
                put("id", it.id)
                put("date", it.date)
                put("programTitle", it.programTitle)
                put("adaptationMode", it.adaptationMode)
                put("completedExercises", it.completedExercises)
                put("energyRating", it.energyRating)
                put("rpeRating", it.rpeRating)
                put("note", it.note)
            }
            arr.put(obj)
        }
        prefs.edit().putString("history", arr.toString()).apply()
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTab == 0 && activeTemplate == null,
                    onClick = { selectedTab = 0; activeTemplate = null },
                    label = { Text("Шаблоны") },
                    icon = { Text("⚡", fontSize = 18.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1 && activeTemplate == null,
                    onClick = { selectedTab = 1; activeTemplate = null },
                    label = { Text("Атлас (1-10)") },
                    icon = { Text("📊", fontSize = 18.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2 && activeTemplate == null,
                    onClick = { selectedTab = 2; activeTemplate = null },
                    label = { Text("Журнал") },
                    icon = { Text("📖", fontSize = 18.sp) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (activeTemplate != null) {
                ActiveWorkoutScreen(
                    template = activeTemplate!!,
                    onFinish = { newEntry ->
                        historyList.add(0, newEntry)
                        saveHistory()
                        activeTemplate = null
                        selectedTab = 2
                    },
                    onCancel = { activeTemplate = null }
                )
            } else {
                when (selectedTab) {
                    0 -> TemplatesTab(onSelectTemplate = { activeTemplate = it })
                    1 -> ExerciseAtlasTab()
                    2 -> HistoryTab(
                        history = historyList,
                        onDelete = { item ->
                            historyList.remove(item)
                            saveHistory()
                        }
                    )
                }
            }
        }
    }
}

// --- ВКЛАДКА 1: ГОТОВЫЕ ШАБЛОНЫ С НАУЧНЫМ УПОРОМ ---

@Composable
fun TemplatesTab(onSelectTemplate: (WorkoutTemplate) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Программы тренировок", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "Калистеника без отягощений. Выберите фокус подготовки под сегодняшнюю цель.",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(BuiltInTemplates) { template ->
            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(template.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        BadgeTag(template.targetCategory)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(template.focusDescription, fontSize = 13.sp, color = Color.LightGray)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Упражнения (${template.exercises.size}):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    template.exercises.forEach { ex ->
                        Text("• ${ex.name} (${ex.equipment})", fontSize = 13.sp, color = Color(0xFFBAC5D5))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onSelectTemplate(template) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Начать тренировку")
                    }
                }
            }
        }
    }
}

// --- ВКЛАДКА 2: АТЛАС УПРАЖНЕНИЙ С МНОГОМЕРНОЙ ОЦЕНКОЙ (1-10) ---

@Composable
fun ExerciseAtlasTab() {
    var selectedFilter by remember { mutableStateOf("Все") }
    val filters = listOf("Все", "Сила", "Функционал", "Кардио", "Кор")

    val filteredList = remember(selectedFilter) {
        when (selectedFilter) {
            "Сила" -> CalisthenicsExerciseDb.filter { it.strengthScore >= 7 }
            "Функционал" -> CalisthenicsExerciseDb.filter { it.functionalScore >= 8 }
            "Кардио" -> CalisthenicsExerciseDb.filter { it.cardioScore >= 7 }
            "Кор" -> CalisthenicsExerciseDb.filter { it.coreScore >= 8 }
            else -> CalisthenicsExerciseDb
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Атлас калистеники", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Многомерный скоринг нагрузки по 10-балльной системе", fontSize = 13.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(12.dp))

        // Фильтры
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(filteredList) { ex ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ex.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Инвентарь: ${ex.equipment}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Шкалы 1-10
                        ScoreBar(label = "Сила", score = ex.strengthScore, color = Color(0xFFEF476F))
                        ScoreBar(label = "Функционал", score = ex.functionalScore, color = Color(0xFF06D6A0))
                        ScoreBar(label = "Кардио", score = ex.cardioScore, color = Color(0xFFFFD166))
                        ScoreBar(label = "Кор / Статика", score = ex.coreScore, color = Color(0xFF118AB2))
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreBar(label: String, score: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.width(95.dp), fontSize = 12.sp, color = Color.LightGray)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF2C313C))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = score / 10f)
                    .background(color)
            )
        }
        Text(
            text = "$score/10",
            modifier = Modifier.width(42.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

// --- ЭКРАН ТРЕНИРОВКИ С АДАПТАЦИЕЙ И ЖУРНАЛОМ САМОЧУВСТВИЯ ---

@Composable
fun ActiveWorkoutScreen(
    template: WorkoutTemplate,
    onFinish: (SessionHistory) -> Unit,
    onCancel: () -> Unit
) {
    // Режим адаптации: 0 = Делоад (-30%), 1 = База (100%), 2 = Прогресс (+15%)
    var adaptationMode by remember { mutableStateOf(1) }
    var currentStep by remember { mutableStateOf(0) } // 0: Тренировка, 1: Журнал самочувствия

    // Оценка самочувствия
    var energyRating by remember { mutableStateOf(3) }
    var rpeRating by remember { mutableStateOf(7) }
    var notes by remember { mutableStateOf("") }

    val adaptationMultiplier = when (adaptationMode) {
        0 -> 0.7f
        2 -> 1.15f
        else -> 1.0f
    }
    val calculatedSets = (template.defaultSets * adaptationMultiplier).roundToInt().coerceAtLeast(1)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(template.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            TextButton(onClick = onCancel) { Text("Выйти", color = Color.Red) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (currentStep == 0) {
            // Блок регуляции нагрузки (Адаптация)
            Text("Авторегуляция готовности:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeButton(
                    title = "Усталость (-30%)",
                    selected = adaptationMode == 0,
                    onClick = { adaptationMode = 0 },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    title = "Норма (100%)",
                    selected = adaptationMode == 1,
                    onClick = { adaptationMode = 1 },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    title = "Драйв (+15%)",
                    selected = adaptationMode == 2,
                    onClick = { adaptationMode = 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "Рабочий план: $calculatedSets подходов по ${template.defaultReps} повторений",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(template.exercises) { ex ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(ex.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Инвентарь: ${ex.equipment}", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MiniScore("Сила", ex.strengthScore)
                                MiniScore("Функц.", ex.functionalScore)
                                MiniScore("Кардио", ex.cardioScore)
                                MiniScore("Кор", ex.coreScore)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { currentStep = 1 },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Завершить и оценить самочувствие")
            }
        } else {
            // ШАГ 2: Журнал восстановления и самочувствия
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text("Журнал самочувствия сессии", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Эти параметры помогают калибровать прогрессию и защищают от перетренированности.", fontSize = 12.sp, color = Color.Gray)
                }

                item {
                    Text("1. Уровень энергии и бодрости (1 - истощен, 5 - полон сил): $energyRating", fontSize = 14.sp)
                    Slider(
                        value = energyRating.toFloat(),
                        onValueChange = { energyRating = it.roundToInt() },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }

                item {
                    Text("2. Воспринимаемая тяжесть RPE (1 - легко, 10 - предельный отказ): $rpeRating", fontSize = 14.sp)
                    Slider(
                        value = rpeRating.toFloat(),
                        onValueChange = { rpeRating = it.roundToInt() },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Заметки (суставы, техника, подручные опоры)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }

            Button(
                onClick = {
                    val modeTitle = when (adaptationMode) {
                        0 -> "Разгрузка (-30%)"
                        2 -> "Прогресс (+15%)"
                        else -> "Базовый объем"
                    }
                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
                    val historyEntry = SessionHistory(
                        id = System.currentTimeMillis(),
                        date = dateStr,
                        programTitle = template.title,
                        adaptationMode = modeTitle,
                        completedExercises = "${template.exercises.size} упражнений ($calculatedSets подх.)",
                        energyRating = energyRating,
                        rpeRating = rpeRating,
                        note = notes.ifBlank { "Без комментариев" }
                    )
                    onFinish(historyEntry)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить в дневник тренировок")
            }
        }
    }
}

@Composable
fun ModeButton(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color.LightGray
        )
    }
}

// --- ВКЛАДКА 3: ИСТОРИЯ И ЖУРНАЛ САМОЧУВСТВИЯ ---

@Composable
fun HistoryTab(history: List<SessionHistory>, onDelete: (SessionHistory) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("История и самочувствие", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Зафиксировано тренировок: ${history.size}", fontSize = 13.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(12.dp))

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Тренировок пока нет. Запустите программу во вкладке «Шаблоны».", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(history, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.programTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(24.dp)) {
                                    Text("✕", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                            Text(item.date, fontSize = 11.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Режим: ${item.adaptationMode} • ${item.completedExercises}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Энергия: ${"★".repeat(item.energyRating)}${"☆".repeat(5 - item.energyRating)}", fontSize = 12.sp, color = Color(0xFFFFD166))
                                Text("Тяжесть RPE: ${item.rpeRating}/10", fontSize = 12.sp, color = Color(0xFFEF476F))
                            }

                            if (item.note.isNotBlank() && item.note != "Без комментариев") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Заметка: ${item.note}", fontSize = 12.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ ---

@Composable
fun BadgeTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MiniScore(label: String, score: Int) {
    Text("$label: $score/10", fontSize = 11.sp, color = Color(0xFF9EABB8))
}
 
