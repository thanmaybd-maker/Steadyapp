package com.thanu.steady.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.thanu.steady.R
import com.thanu.steady.di.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ExpandedNavigation(container: AppContainer, access: AccessState,
    onAuthentication: ((() -> Unit)?) -> Unit, onTheme: (com.thanu.steady.data.ExpandedProfile) -> Unit,
    onSafetyVisibility: (Boolean) -> Unit = {}, requestedRoute: String? = null, onReminderConsumed: () -> Unit = {}) {
    val model: ExpandedViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ExpandedViewModel(container.expandedRepository, container.activityRepository, container.preferencesRepository,
                container.activityAlarms, container.notificationAdapter, container.bootstrap,
                container.platformSensors, container.audioSoundscapeEngine,
                { container.isForeground },
                { container.routineReminders.refresh() }, { container.timerRepository.reconcile(true).forEach { container.alarmAdapter.cancelAlarm(it.id) } }) as T
        }
    })
    val state by model.state.collectAsState()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val tab = entry?.destination?.route ?: "today"
    var publicSafety by rememberSaveable { mutableStateOf(false) }
    var privateSafety by rememberSaveable { mutableStateOf(false) }
    SideEffect { onSafetyVisibility(publicSafety || privateSafety || tab == "safety") }
    DisposableEffect(Unit) { onDispose { onSafetyVisibility(false) } }
    val safety = { publicSafety = true }
    val profile = state.period?.profile
    LaunchedEffect(profile) { profile?.let(onTheme) }
    val tabs = listOf("today" to R.string.today_tab, "health" to R.string.health_tab,
        "focus" to R.string.focus_tab, "review" to R.string.review_tab, "settings" to R.string.settings_tab)
    fun navigate(route: String) { nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true; restoreState = true
    } }
    LaunchedEffect(profile?.onboarded, requestedRoute) {
        if(profile?.onboarded == true && requestedRoute in setOf("today", "focus")) {
            navigate(requireNotNull(requestedRoute)); onReminderConsumed()
        }
    }
    val largeText = LocalDensity.current.fontScale > 1.4f
    val wide = LocalConfiguration.current.screenWidthDp >= 600 && !largeText
    var chooseTab by remember { mutableStateOf(false) }
    var addMenu by remember { mutableStateOf(false) }
    var quickAction by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = {
        if(largeText) Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 12.dp)) {
            Text(stringResource(tabs.firstOrNull { it.first == tab }?.second ?: R.string.app_name),style = MaterialTheme.typography.titleLarge)
            SecondaryAction(R.string.safety_action) { privateSafety = true }
        } else
        TopAppBar(title = { Text(stringResource(tabs.firstOrNull { it.first == tab }?.second ?: R.string.app_name)) },
            actions = { TextButton(onClick = { privateSafety = true }, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.safety_action)) } })
    }, floatingActionButton = {
        if(profile?.onboarded == true) Box {
            FloatingActionButton(onClick = { addMenu = true },modifier = Modifier.sizeIn(minWidth = 56.dp,minHeight = 56.dp)) { Text(stringResource(R.string.add_menu)) }
            DropdownMenu(addMenu,{ addMenu = false }) {
                listOf(Triple("today","task",R.string.add_task),Triple("today","habit",R.string.add_habit),
                    Triple("today","capture",R.string.capture_idea),Triple("health","workout",R.string.start_workout),
                    Triple("health","water",R.string.log_water),Triple("health","sleep",R.string.log_sleep)).forEach { (route,action,label) ->
                    DropdownMenuItem(text = { Text(stringResource(label)) },modifier = Modifier.heightIn(min = 56.dp),
                        onClick = { addMenu = false; quickAction = action; navigate(route) })
                }
            }
        }
    }, bottomBar = {
        if (largeText) {
            Column(Modifier.padding(8.dp)) {
                PrimaryAction(R.string.choose_tab) { chooseTab = true }
                DropdownMenu(chooseTab, onDismissRequest = { chooseTab = false }) {
                    tabs.forEach { (route, label) -> DropdownMenuItem(text = { Text(stringResource(label)) },
                        modifier = Modifier.heightIn(min = 56.dp), onClick = { chooseTab = false; navigate(route) }) }
                }
            }
        } else if (!wide) NavigationBar {
            tabs.forEachIndexed { index, (route, label) -> NavigationBarItem(selected = tab == route,
                onClick = { navigate(route) }, modifier = Modifier.heightIn(min = 56.dp),
                icon = { Text(listOf("◉", "♡", "◷", "▦", "⚙")[index]) }, label = { Text(stringResource(label)) }) }
        }
    }) { padding ->
        Row(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (wide) NavigationRail {
                tabs.forEach { (route,label) -> NavigationRailItem(selected = tab == route,onClick = { navigate(route) },
                    modifier = Modifier.heightIn(min=56.dp),icon = { Text(stringResource(label)) }) }
            }
        Box(Modifier.weight(1f)) {
            if (state.loading && profile == null) ExpandedPage { StateMessages(state); SecondaryAction(R.string.retry) { model.reload() } }
            else if (profile == null) ExpandedPage { StateMessages(state); PublicSafetyPanel(access.bootstrap.country); SecondaryAction(R.string.retry) { model.reload() } }
            else if (!profile.onboarded) ExpandedOnboarding(model, state, safety)
            else NavHost(nav, startDestination = "today") {
                composable("today") { LaunchedEffect(Unit) { model.reload() }; ExpandedToday(model, state, safety, { navigate("focus") }, { navigate("health") },quickAction) { quickAction = null } }
                composable("health") { LaunchedEffect(Unit) { model.reload() }; ExpandedHealth(model, state, safety,quickAction) { quickAction = null } }
                composable("focus") { LaunchedEffect(Unit) { model.reload() }; ExpandedFocus(model, state, safety) }
                composable("review") { ExpandedReview(model, state, safety) }
                composable("settings") { ExpandedSettings(model, state, container, access, onAuthentication, safety) { navigate("usage_insights") } }
                composable("usage_insights") {
                    ExpandedUsageInsights(model, state, safety) { nav.popBackStack() }
                }
                composable("learn_build") { ExpandedLearnBuildModules() }
                composable("people") { ExpandedPeople() }
                composable("money_guard") { ExpandedMoneyGuard() }
                composable("voice_ocr") { ExpandedVoiceOcr() }
                composable("safety") {
                    val safetyModel: SafetyViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST") return SafetyViewModel(container.privateSafetyRepository, container.clock) as T
                        }
                    })
                    SafetyScreen(safetyModel, access.bootstrap.country)
                }
            }
        }
        }
    }
    if (privateSafety) Dialog(onDismissRequest = { privateSafety = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
        securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn)) {
        val safetyModel: SafetyViewModel = viewModel(key = "global_private_safety", factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST") return SafetyViewModel(container.privateSafetyRepository,container.clock) as T
            }
        })
        DialogSurface {
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) { SecondaryAction(R.string.close_action) { privateSafety = false }; Box(Modifier.weight(1f)) { SafetyScreen(safetyModel,access.bootstrap.country) } }
        }
    }
    if (publicSafety) Dialog(onDismissRequest = { publicSafety = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        DialogSurface { Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) { ExpandedPage {
            SecondaryAction(R.string.close_action) { publicSafety = false }
            PublicSafetyPanel(access.bootstrap.country)
        } } }
    }
}

@Composable fun ExpandedOnboarding(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit) {
    val key = "onboarding"
    LaunchedEffect(Unit) { model.openDraft(key, mapOf("name" to "", "country" to "IN", "zone" to "Asia/Kolkata", "boundary" to "04:00", "palette" to "KINETIC",
        "modules" to "PLAN,HABITS,FOCUS,MOVEMENT,FOOD,WATER,SLEEP","contrast" to "false","motion" to "false","large" to "false")) }
    val drafts by model.drafts.collectAsState()
    val form = drafts[key] ?: emptyMap()
    ExpandedPage {
        SectionCard(R.string.welcome_title) {
            Text(stringResource(R.string.welcome_description))
            SecondaryAction(R.string.safety_action, onClick = onSafety)
            TextInput(form["name"] ?: "", R.string.display_name, { model.field(key, "name", it) })
            ChoiceList(form["country"] ?: "IN", listOf("IN" to R.string.country_india, "OTHER" to R.string.country_other)) { model.field(key, "country", it) }
            ChoiceList(form["palette"] ?: "KINETIC", listOf("KINETIC" to R.string.kinetic_palette, "DAYBOOK" to R.string.daybook_palette)) { model.field(key, "palette", it) }
            TextInput(form["zone"] ?: "", R.string.timezone, { model.field(key, "zone", it) })
            TextInput(form["boundary"] ?: "", R.string.day_boundary, { model.field(key, "boundary", it) })
            ToggleRow(R.string.high_contrast,form["contrast"] == "true") { model.field(key,"contrast",it.toString()) }
            ToggleRow(R.string.reduced_motion,form["motion"] == "true") { model.field(key,"motion",it.toString()) }
            ToggleRow(R.string.larger_text,form["large"] == "true") { model.field(key,"large",it.toString()) }
            val modules = form["modules"]?.split(',')?.filter(String::isNotBlank)?.toSet() ?: state.period!!.profile.modules.split(',').toSet()
            listOf("PLAN" to R.string.plan_module,"HABITS" to R.string.habits_title,"FOCUS" to R.string.focus_tab,
                "MOVEMENT" to R.string.movement_title,"FOOD" to R.string.food_title,"WATER" to R.string.water_title,"SLEEP" to R.string.sleep_title).forEach { (id,label) ->
                ToggleRow(label,id in modules) { checked -> model.field(key,"modules",(if(checked) modules+id else modules-id).joinToString(",")) }
            }
            StateMessages(state)
            PrimaryAction(R.string.finish_setup, !state.busy) {
                model.action({
                    val boundary = java.time.LocalTime.parse(form["boundary"] ?: "04:00").let { it.hour * 60 + it.minute }
                    // Use the same persistence action as Settings; no selected defaults become activity logs.
                    val p = state.period!!.profile.copy(displayName = form["name"] ?: "", country = form["country"] ?: "IN",
                        palette = form["palette"] ?: "KINETIC", onboarded = true,modules = modules.joinToString(","),
                        highContrast = form["contrast"] == "true",reducedMotion = form["motion"] == "true",textScale = if(form["large"] == "true") 1.25f else 1f)
                    model.completeOnboarding(p, form["zone"] ?: "Asia/Kolkata", boundary)
                }, after = { model.clearDraft(key); model.reload() })
            }
        }
    }
}
