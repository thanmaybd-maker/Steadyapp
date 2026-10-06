package com.thanu.steady.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.thanu.steady.R
import com.thanu.steady.data.*
import kotlinx.coroutines.CancellationException

@Composable fun SubjectTopics(model: ExpandedViewModel,state: ExpandedUiState,onSafety: () -> Unit) {
    val subjects = state.period?.subjects ?: return
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var editingTopic by remember { mutableStateOf<Topic?>(null) }
    var topicSubject by remember { mutableStateOf<Subject?>(null) }
    var topics by remember { mutableStateOf<Map<String,List<Topic>>>(emptyMap()) }
    LaunchedEffect(subjects,state.busy) {
        try { topics = subjects.associate { it.id to model.repository.topics(it.id) } }
        catch(cancelled: CancellationException) { throw cancelled }
        catch(_: Exception) { model.action({ error("Storage unavailable") }) }
    }
    SectionCard(R.string.topics_title) {
        subjects.forEach { subject ->
            Text(subject.title)
            SecondaryAction(R.string.edit_action) { editingSubject = subject }
            SecondaryAction(if(subject.archived) R.string.restore_subject else R.string.archive_subject,!state.busy) {
                model.action({ model.repository.saveSubject(subject.copy(archived = !subject.archived)) })
            }
            if(!subject.archived) SecondaryAction(R.string.add_topic) { topicSubject = subject; editingTopic = null }
            topics[subject.id].orEmpty().forEach { topic ->
                Text(topic.title)
                ChoiceList(topic.state,listOf("NEW" to R.string.topic_new,"ACTIVE" to R.string.topic_active,"DONE" to R.string.topic_done)) {
                    model.action({ model.repository.saveTopic(topic.copy(state = it)) })
                }
                SecondaryAction(R.string.edit_action) { topicSubject = subject; editingTopic = topic }
            }
        }
    }
    editingSubject?.let { subject ->
        val key = "subject:${subject.id}"
        DraftEditor(model,state,key,R.string.subject_title,mapOf("title" to subject.title,"code" to subject.code),onSafety,{ editingSubject = null }) { values,close ->
            TextInput(values["title"].orEmpty(),R.string.subject_title,{ model.field(key,"title",it) })
            TextInput(values["code"].orEmpty(),R.string.subject_code,{ model.field(key,"code",it) })
            PrimaryAction(R.string.save_action,!state.busy) { model.action({ model.repository.saveSubject(subject.copy(title = values["title"].orEmpty(),code = values["code"].orEmpty())) },after = close) }
        }
    }
    topicSubject?.let { subject ->
        val key = "topic:${editingTopic?.id ?: subject.id}"
        DraftEditor(model,state,key,R.string.topic_title,mapOf("title" to editingTopic?.title.orEmpty()),onSafety,{ topicSubject = null }) { values,close ->
            TextInput(values["title"].orEmpty(),R.string.topic_title,{ model.field(key,"title",it) })
            PrimaryAction(R.string.save_action,!state.busy) { model.action({ model.repository.saveTopic(Topic(editingTopic?.id ?: model.repository.newId(),subject.id,
                values["title"].orEmpty(),editingTopic?.state ?: "NEW")) },after = close) }
        }
    }
}
