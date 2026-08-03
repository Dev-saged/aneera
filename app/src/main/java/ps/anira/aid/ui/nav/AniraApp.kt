package ps.anira.aid.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import ps.anira.aid.backup.TelegramSettingsStore
import ps.anira.aid.data.MonthKey
import ps.anira.aid.data.Repository
import ps.anira.aid.ui.archive.ArchiveScreen
import ps.anira.aid.ui.records.RecordsScreen
import ps.anira.aid.ui.register.RegisterScreen
import ps.anira.aid.ui.settings.SettingsScreen
import ps.anira.aid.ui.theme.ThemePreferenceStore

private object Routes {
    const val REGISTER = "register"
    const val RECORDS = "records"
    const val ARCHIVE = "archive"
    const val SETTINGS = "settings"
    const val MONTH_DETAIL = "month/{key}"
    fun monthDetail(key: String) = "month/$key"
}

/**
 * الشاشة الرئيسية الموحَّدة: شريط علوي (عنوان + زر إعدادات تيليجرام، يعادل
 * زر الترس بهيدر الويب) فوق شريط تنقّل سفلي بثلاث وجهات رئيسية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniraApp(
    repo: Repository,
    tgStore: TelegramSettingsStore,
    themeStore: ThemePreferenceStore,
    isDark: Boolean
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val currentMonthCount by repo.observeCurrentCount(MonthKey.currentKey()).collectAsState(initial = 0)
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أنيرا") },
                actions = {
                    IconButton(onClick = { scope.launch { themeStore.setDark(!isDark) } }) {
                        Icon(
                            if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "تبديل المظهر"
                        )
                    }
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "إعدادات تيليجرام")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.REGISTER,
                    onClick = { navController.navigateToTab(Routes.REGISTER) },
                    icon = { Icon(Icons.Filled.Bolt, contentDescription = null) },
                    label = { Text("الإدخال السريع") }
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.RECORDS,
                    onClick = { navController.navigateToTab(Routes.RECORDS) },
                    icon = {
                        if (currentMonthCount > 0) {
                            BadgedBox(badge = { Badge { Text("$currentMonthCount") } }) {
                                Icon(Icons.Filled.ListAlt, contentDescription = null)
                            }
                        } else {
                            Icon(Icons.Filled.ListAlt, contentDescription = null)
                        }
                    },
                    label = { Text("السجلات") }
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.ARCHIVE || currentRoute == Routes.MONTH_DETAIL,
                    onClick = { navController.navigateToTab(Routes.ARCHIVE) },
                    icon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                    label = { Text("الأرشيف") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.REGISTER,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.REGISTER) { RegisterScreen(repo = repo) }
            composable(Routes.RECORDS) {
                RecordsScreen(repo = repo, monthKey = MonthKey.currentKey(), monthLabel = MonthKey.label(MonthKey.currentKey()))
            }
            composable(Routes.ARCHIVE) {
                ArchiveScreen(repo = repo, onOpenMonth = { key, _ ->
                    navController.navigate(Routes.monthDetail(key))
                })
            }
            composable(Routes.MONTH_DETAIL) { backStackEntry ->
                val key = backStackEntry.arguments?.getString("key") ?: MonthKey.currentKey()
                RecordsScreen(repo = repo, monthKey = key, monthLabel = MonthKey.label(key))
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(repo = repo, store = tgStore)
            }
        }
    }
}

private fun androidx.navigation.NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
