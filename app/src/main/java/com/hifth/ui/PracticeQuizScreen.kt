package com.hifth.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.hifth.data.QuranVerse
import com.hifth.data.SavedVocabulary

@Composable
internal fun PracticeQuizScreen(
    verses: List<QuranVerse>,
    savedWords: List<SavedVocabulary>,
    onAttempt: (String, Int) -> Unit
) {
    var mode by remember(verses, savedWords) { mutableStateOf(PracticeQuizMode.CONTINUATION) }
    var questions by remember(verses, savedWords) { mutableStateOf(emptyList<PracticeQuizQuestion>()) }
    var questionIndex by remember(verses, savedWords) { mutableIntStateOf(0) }
    var selectedAnswer by remember(verses, savedWords) { mutableStateOf<String?>(null) }
    var score by remember(verses, savedWords) { mutableIntStateOf(0) }
    val availableQuestions = remember(mode, verses, savedWords) { buildPracticeQuiz(mode, verses, savedWords) }
    val question = questions.getOrNull(questionIndex)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (questions.isEmpty()) {
            Text("Choose a quick quiz", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PracticeQuizMode.entries.forEach { quizMode ->
                    FilterChip(
                        selected = mode == quizMode,
                        onClick = { mode = quizMode },
                        label = { Text(quizMode.label, maxLines = 1) }
                    )
                }
            }
            Text(
                when (mode) {
                    PracticeQuizMode.CONTINUATION -> "Recall how each ayah continues."
                    PracticeQuizMode.WORD_MEANING -> "Review glosses from loaded ayahs and saved words."
                    PracticeQuizMode.TRANSLATION -> "Match a loaded ayah to its translation."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { questions = availableQuestions; questionIndex = 0; score = 0 },
                enabled = availableQuestions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start ${availableQuestions.size.coerceAtMost(10)} questions") }
            if (availableQuestions.isEmpty()) {
                Text("Load more Quran content or save translated words to use this quiz.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (question == null) {
            Text("Quiz complete", style = MaterialTheme.typography.titleMedium)
            Text("$score of ${questions.size} correct · ${score * 100 / questions.size}%", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { questionIndex = 0; selectedAnswer = null; score = 0 }) { Text("Try again") }
                OutlinedButton(onClick = { questions = emptyList(); selectedAnswer = null }) { Text("Choose mode") }
            }
        } else {
            Text("Question ${questionIndex + 1} of ${questions.size} · ${question.verseKey}", style = MaterialTheme.typography.labelLarge)
            Text(question.promptArabic, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Rtl))
            Text(question.prompt, style = MaterialTheme.typography.bodyLarge)
            question.options.forEach { option ->
                val containerColor = when {
                    selectedAnswer == null -> MaterialTheme.colorScheme.surface
                    option == question.answer -> MaterialTheme.colorScheme.secondaryContainer
                    option == selectedAnswer -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surface
                }
                OutlinedButton(
                    onClick = {
                        if (selectedAnswer == null) {
                            selectedAnswer = option
                            val correct = option == question.answer
                            if (correct) score++
                            onAttempt(question.verseKey, if (correct) 0 else 100)
                        }
                    },
                    enabled = selectedAnswer == null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = containerColor)
                ) {
                    Text(
                        option,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (mode == PracticeQuizMode.CONTINUATION) TextAlign.End else TextAlign.Start,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = if (mode == PracticeQuizMode.CONTINUATION) TextDirection.Rtl else TextDirection.Ltr
                        )
                    )
                }
            }
            selectedAnswer?.let { answer ->
                Text(
                    if (answer == question.answer) "Correct" else "Correct answer: ${question.answer}",
                    color = if (answer == question.answer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = { questionIndex++; selectedAnswer = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (questionIndex == questions.lastIndex) "See score" else "Next") }
            }
        }
    }
}