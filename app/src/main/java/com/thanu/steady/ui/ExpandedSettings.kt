package com.thanu.steady.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thanu.steady.R
import com.thanu.steady.di.AppContainer

@Composable fun ExpandedSettings(model: ExpandedViewModel, state: ExpandedUiState, container: AppContainer,
    access: AccessState, onAuthentication: ((() -> Unit)?) -> Unit, onSafety: () -> Unit, onUsage: () -> Unit) {
    val period = state.period ?: return
    val profile = period.profile
    val context = LocalContext.current
    var files by remember { mutableStateOf(false) }
    var timeEditor by remember { mutableStateOf(false) }
    var goals by remember { mutableStateOf(false) }
    var personal by remember { mutableStateOf(false) }
    var reminders by remember { mutableStateOf(false) }
    var notices by remember { mutableStateOf(false) }
    var summaries by remember { mutableStateOf(false) }
    if(notices) { LicensesScreen({ notices = false },onSafety); return }
    if (files) {
        val settingsModel: SettingsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return SettingsViewModel(container.recoveryRepository, container.documentAdapter,
                    container.alarmAdapter, container.notificationAdapter, container.clock, container::deleteLocalData,
                    container.activityAlarms, { container.bootstrap.clear() }) as T
            }
        })
        SettingsScreen(settingsModel, onBack = { files = false }) { files = false; model.afterRecovery() }
        return
    }
    ExpandedPage {
        StateMessages(state)
        SectionCard(R.string.profile_settings) {
            Text(profile.displayName.ifBlank { stringResource(R.string.none_recorded) })
            SecondaryAction(R.string.edit_profile) { personal = true }
        }
        SectionCard(R.string.appearance_title) {
            ChoiceList(profile.palette, listOf("KINETIC" to R.string.kinetic_palette, "DAYBOOK" to R.string.daybook_palette)) { model.updateProfile(profile.copy(palette = it)) }
            ChoiceList(profile.theme, listOf("SYSTEM" to R.string.system_theme, "LIGHT" to R.string.light_theme, "DARK" to R.string.dark_theme)) { model.updateProfile(profile.copy(theme = it)) }
            ToggleRow(R.string.high_contrast, profile.highContrast) { model.updateProfile(profile.copy(highContrast = it)) }
            ToggleRow(R.string.reduced_motion, profile.reducedMotion) { model.updateProfile(profile.copy(reducedMotion = it)) }
            ToggleRow(R.string.larger_text, profile.textScale > 1f) { model.updateProfile(profile.copy(textScale = if (it) 1.25f else 1f)) }
        }
        SectionCard(R.string.modules_title) {
            SecondaryAction(R.string.summary_preferences) { summaries = true }
            val enabled = profile.modules.split(',').toSet()
            listOf("PLAN" to R.string.plan_module, "HABITS" to R.string.habits_title, "FOCUS" to R.string.focus_tab,
                "MOVEMENT" to R.string.movement_title, "FOOD" to R.string.food_title, "WATER" to R.string.water_title, "SLEEP" to R.string.sleep_title).forEach { (id, label) ->
                ToggleRow(label, id in enabled) { checked -> model.updateProfile(profile.copy(modules = (if (checked) enabled + id else enabled - id).joinToString(","))) }
            }
            val cards = profile.dashboard.split(',').filter(String::isNotBlank)
            listOf("NEXT" to R.string.next_action_title, "RINGS" to R.string.today_summaries, "TIMELINE" to R.string.timeline_title,
                "HABITS" to R.string.habits_title, "CAPTURE" to R.string.capture_title, "FOOD" to R.string.food_title,"ROUTINES" to R.string.routine_anchors).forEach { (id, label) ->
                ToggleRow(label, id in cards) { checked -> model.updateProfile(profile.copy(dashboard = (if (checked) cards + id else cards - id).joinToString(","))) }
                val index = cards.indexOf(id)
                if (index >= 0) {
                    val roomy = profile.wideCards.split(',').filter(String::isNotBlank).toSet()
                    ToggleRow(R.string.roomy_card,id in roomy) { enabled -> model.updateProfile(profile.copy(wideCards = (if(enabled) roomy+id else roomy-id).joinToString(","))) }
                    SecondaryAction(R.string.move_up, index > 0) { val changed = cards.toMutableList(); java.util.Collections.swap(changed, index, index - 1); model.updateProfile(profile.copy(dashboard = changed.joinToString(","))) }
                    SecondaryAction(R.string.move_down, index < cards.lastIndex) { val changed = cards.toMutableList(); java.util.Collections.swap(changed, index, index + 1); model.updateProfile(profile.copy(dashboard = changed.joinToString(","))) }
                }
            }
        }
        SectionCard(R.string.time_settings) {
            Text(period.preferences.zoneId)
            ToggleRow(R.string.follow_device_zone,profile.zoneMode == "DEVICE") { enabled -> model.action({
                model.repository.saveProfile(profile.copy(zoneMode = if(enabled) "DEVICE" else "FIXED"))
            },after = { model.reload() }) }
            SecondaryAction(R.string.edit_time_policy) { timeEditor = true }
            SecondaryAction(R.string.edit_optional_targets) { goals = true }
            Text(stringResource(R.string.historical_policy_notice))
            SecondaryAction(R.string.reminder_settings) { reminders = true }
        }
        SectionCard(R.string.permissions_title) {
            Text(stringResource(R.string.permissions_disclosure))
            SecondaryAction(R.string.usage_insights_title, onClick = onUsage)
            SecondaryAction(R.string.open_android_permissions) { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
            if (android.os.Build.VERSION.SDK_INT >= 31) SecondaryAction(R.string.open_exact_access) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }
        }
        SectionCard(R.string.privacy_lock_title) {
            ToggleRow(R.string.require_app_lock, access.bootstrap.locked) { enabled ->
                if (enabled) onAuthentication { model.action({ container.bootstrap.setLock(true) }) }
                else model.action({ container.bootstrap.setLock(false) })
            }
            ToggleRow(R.string.hide_recents, access.bootstrap.hideRecents) { model.action({ container.bootstrap.setRecents(it) }) }
            Text(stringResource(R.string.lock_relock_description))
            SecondaryAction(R.string.portable_files) { files = true }
        }
        SectionCard(R.string.about_title) {
            Text(stringResource(R.string.about_offline))
            Text(stringResource(R.string.no_demo_records))
            Text(stringResource(R.string.technical_diagnostics),style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.diagnostics_description))
            SecondaryAction(R.string.third_party_notices) { notices = true }
            SecondaryAction(R.string.safety_action, onClick = onSafety)
        }
    }
    if(summaries) DashboardSettingsEditor(model,state,onSafety) { summaries = false }
    if(personal) DraftEditor(model,state,"profile",R.string.profile_settings,mapOf("name" to profile.displayName,"country" to profile.country,"date" to profile.dateStyle),onSafety,{ personal = false }) { values,close ->
        TextInput(values["name"].orEmpty(),R.string.display_name,{ model.field("profile","name",it) })
        ChoiceList(values["country"] ?: "IN",listOf("IN" to R.string.country_india,"OTHER" to R.string.country_other)) { model.field("profile","country",it) }
        ChoiceList(values["date"] ?: "LOCAL",listOf("LOCAL" to R.string.date_local,"ISO" to R.string.date_iso)) { model.field("profile","date",it) }
        PrimaryAction(R.string.save_action,!state.busy) { model.action({
            model.repository.saveProfile(profile.copy(displayName = values["name"].orEmpty(),country = values["country"] ?: "IN",dateStyle = values["date"] ?: "LOCAL"))
            container.bootstrap.setCountry(values["country"] ?: "IN")
        },after = close) }
    }
    if(reminders) ReminderSettingsEditor(model,state,onSafety) { reminders = false }
    if (timeEditor) DraftEditor(model, state, "time_policy", R.string.time_settings, mapOf("zone" to period.preferences.zoneId,
        "boundary" to "%02d:%02d".format(java.util.Locale.ROOT,period.preferences.boundaryMinutes/60,period.preferences.boundaryMinutes%60)), onSafety, { timeEditor = false }) { values, close ->
        TextInput(values["zone"].orEmpty(), R.string.timezone, { model.field("time_policy", "zone", it) })
        TextInput(values["boundary"].orEmpty(), R.string.day_boundary, { model.field("time_policy", "boundary", it) })
        PrimaryAction(R.string.save_action, !state.busy) { model.action({ val time = java.time.LocalTime.parse(values["boundary"]!!)
            model.setTimePolicy(values["zone"]!!, time.hour*60+time.minute)
            model.repository.saveProfile(profile.copy(zoneMode = "FIXED")) }, after = { close(); model.reload() }) }
    }
    if (goals) DraftEditor(model, state, "optional_targets", R.string.edit_optional_targets, mapOf("focus" to (profile.focusTargetMinutes?.toString() ?: ""),
        "water" to (profile.waterTargetMl?.toString() ?: ""), "steps" to (profile.stepTarget?.toString() ?: ""), "avoid" to period.preferences.avoidFoods,
        "vegetarian" to period.preferences.vegetarian.toString()), onSafety, { goals = false }) { values, close ->
        TextInput(values["focus"].orEmpty(), R.string.focus_target_optional, { model.field("optional_targets", "focus", it) })
        TextInput(values["water"].orEmpty(), R.string.water_target_optional, { model.field("optional_targets", "water", it) })
        TextInput(values["steps"].orEmpty(), R.string.steps_target_optional, { model.field("optional_targets", "steps", it) })
        TextInput(values["avoid"].orEmpty(), R.string.avoid_foods, { model.field("optional_targets", "avoid", it) })
        ToggleRow(R.string.vegetarian_food, values["vegetarian"] == "true") { model.field("optional_targets", "vegetarian", it.toString()) }
        PrimaryAction(R.string.save_action, !state.busy) { model.action({
            model.repository.saveProfile(profile.copy(focusTargetMinutes = values["focus"]?.takeIf(String::isNotBlank)?.toInt(),
                waterTargetMl = values["water"]?.takeIf(String::isNotBlank)?.toInt(), stepTarget = values["steps"]?.takeIf(String::isNotBlank)?.toLong()))
            model.setFoodPreferences(values["vegetarian"] == "true", values["avoid"].orEmpty())
        }, after = close) }
    }
}
