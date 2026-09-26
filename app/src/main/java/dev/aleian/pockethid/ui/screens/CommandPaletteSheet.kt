package dev.aleian.pockethid.ui.screens

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.SurfaceCard
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PaletteCommand(
    val id: String,
    val title: String,
    val category: String,
    val shortcutDisplay: String,
    val icon: ImageVector,
    val keywords: List<String>,
    val action: suspend (InputTransport) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandPaletteSheet(
    transport: InputTransport?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Shortcuts", "System", "Virtual Desktops", "Coding", "Media", "Presentation")

    val allCommands = remember {
        listOf(
            // Clipboard & Edit
            PaletteCommand(
                id = "copy",
                title = "Copy",
                category = "Shortcuts",
                shortcutDisplay = "Ctrl + C",
                icon = Icons.Default.ContentCopy,
                keywords = listOf("copy", "clipboard", "duplicate"),
                action = { it.sendKeyClick(HidConstants.KEY_C, HidConstants.MOD_LEFT_CTRL) }
            ),
            PaletteCommand(
                id = "paste",
                title = "Paste",
                category = "Shortcuts",
                shortcutDisplay = "Ctrl + V",
                icon = Icons.Default.ContentCopy,
                keywords = listOf("paste", "clipboard", "insert"),
                action = { it.sendKeyClick(HidConstants.KEY_V, HidConstants.MOD_LEFT_CTRL) }
            ),
            PaletteCommand(
                id = "cut",
                title = "Cut",
                category = "Shortcuts",
                shortcutDisplay = "Ctrl + X",
                icon = Icons.Default.ContentCopy,
                keywords = listOf("cut", "clipboard", "move"),
                action = { it.sendKeyClick(HidConstants.KEY_X, HidConstants.MOD_LEFT_CTRL) }
            ),
            PaletteCommand(
                id = "undo",
                title = "Undo",
                category = "Shortcuts",
                shortcutDisplay = "Ctrl + Z",
                icon = Icons.Default.Keyboard,
                keywords = listOf("undo", "revert", "back"),
                action = { it.sendKeyClick(HidConstants.KEY_Z, HidConstants.MOD_LEFT_CTRL) }
            ),
            PaletteCommand(
                id = "redo",
                title = "Redo",
                category = "Shortcuts",
                shortcutDisplay = "Ctrl + Y",
                icon = Icons.Default.Keyboard,
                keywords = listOf("redo", "repeat"),
                action = { it.sendKeyClick(HidConstants.KEY_Y, HidConstants.MOD_LEFT_CTRL) }
            ),
            PaletteCommand(
                id = "select_all",
                title = "Select All",
                category = "Shortcuts",
                shortcutDisplay = "Ctrl + A",
                icon = Icons.Default.Keyboard,
                keywords = listOf("select", "all", "highlight"),
                action = { it.sendKeyClick(HidConstants.KEY_A, HidConstants.MOD_LEFT_CTRL) }
            ),
            PaletteCommand(
                id = "save",
                title = "Save File",
                category = "Shortcuts",
                shortcutDisplay = "Ctrl + S",
                icon = Icons.Default.Keyboard,
                keywords = listOf("save", "write", "disk"),
                action = { it.sendKeyClick(HidConstants.KEY_S, HidConstants.MOD_LEFT_CTRL) }
            ),
            PaletteCommand(
                id = "find",
                title = "Find / Search",
                category = "Shortcuts",
                shortcutDisplay = "Ctrl + F",
                icon = Icons.Default.Search,
                keywords = listOf("find", "search", "lookup"),
                action = { it.sendKeyClick(HidConstants.KEY_F, HidConstants.MOD_LEFT_CTRL) }
            ),

            // System & Window Management
            PaletteCommand(
                id = "alt_tab",
                title = "Switch Window",
                category = "System",
                shortcutDisplay = "Alt + Tab",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("switch", "window", "alt", "tab", "app"),
                action = { it.sendKeyClick(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_ALT) }
            ),
            PaletteCommand(
                id = "show_desktop",
                title = "Show Desktop",
                category = "System",
                shortcutDisplay = "Win + D",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("desktop", "minimize", "hide", "all"),
                action = { it.sendKeyClick(HidConstants.KEY_D, HidConstants.MOD_LEFT_GUI) }
            ),
            PaletteCommand(
                id = "task_view",
                title = "Task View / Overview",
                category = "System",
                shortcutDisplay = "Win + Tab",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("task", "view", "overview", "windows", "timeline"),
                action = { it.sendKeyClick(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_GUI) }
            ),
            PaletteCommand(
                id = "screenshot",
                title = "Screenshot (Snipping Tool)",
                category = "System",
                shortcutDisplay = "Win + Shift + S",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("screenshot", "capture", "snip", "screen"),
                action = {
                    it.sendKeyClick(
                        HidConstants.KEY_S,
                        (HidConstants.MOD_LEFT_GUI.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                    )
                }
            ),
            PaletteCommand(
                id = "lock_pc",
                title = "Lock Computer",
                category = "System",
                shortcutDisplay = "Win + L",
                icon = Icons.Default.PowerSettingsNew,
                keywords = listOf("lock", "security", "screen", "sleep"),
                action = { it.sendKeyClick(HidConstants.KEY_L, HidConstants.MOD_LEFT_GUI) }
            ),
            PaletteCommand(
                id = "task_manager",
                title = "Task Manager",
                category = "System",
                shortcutDisplay = "Ctrl + Shift + Esc",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("task", "manager", "kill", "processes"),
                action = {
                    it.sendKeyClick(
                        HidConstants.KEY_ESC,
                        (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                    )
                }
            ),
            PaletteCommand(
                id = "file_explorer",
                title = "File Explorer",
                category = "System",
                shortcutDisplay = "Win + E",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("explorer", "files", "folder", "documents"),
                action = { it.sendKeyClick(HidConstants.KEY_E, HidConstants.MOD_LEFT_GUI) }
            ),
            PaletteCommand(
                id = "run_dialog",
                title = "Run Dialog",
                category = "System",
                shortcutDisplay = "Win + R",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("run", "dialog", "execute", "cmd"),
                action = { it.sendKeyClick(HidConstants.KEY_R, HidConstants.MOD_LEFT_GUI) }
            ),
            PaletteCommand(
                id = "quick_settings",
                title = "Action Center / Quick Settings",
                category = "System",
                shortcutDisplay = "Win + A",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("action", "center", "quick", "settings", "wifi", "sound"),
                action = { it.sendKeyClick(HidConstants.KEY_A, HidConstants.MOD_LEFT_GUI) }
            ),
            PaletteCommand(
                id = "notifications",
                title = "Notification Center",
                category = "System",
                shortcutDisplay = "Win + N",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("notification", "calendar", "alerts"),
                action = { it.sendKeyClick(HidConstants.KEY_N, HidConstants.MOD_LEFT_GUI) }
            ),
            PaletteCommand(
                id = "snap_left",
                title = "Snap Window Left",
                category = "System",
                shortcutDisplay = "Win + Left",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("snap", "left", "window", "split"),
                action = { it.sendKeyClick(HidConstants.KEY_LEFT, HidConstants.MOD_LEFT_GUI) }
            ),
            PaletteCommand(
                id = "snap_right",
                title = "Snap Window Right",
                category = "System",
                shortcutDisplay = "Win + Right",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("snap", "right", "window", "split"),
                action = { it.sendKeyClick(HidConstants.KEY_RIGHT, HidConstants.MOD_LEFT_GUI) }
            ),
            PaletteCommand(
                id = "maximize",
                title = "Maximize Window",
                category = "System",
                shortcutDisplay = "Win + Up",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("maximize", "full", "window"),
                action = { it.sendKeyClick(HidConstants.KEY_UP, HidConstants.MOD_LEFT_GUI) }
            ),
            PaletteCommand(
                id = "close_window",
                title = "Close Active Window",
                category = "System",
                shortcutDisplay = "Alt + F4",
                icon = Icons.Default.PowerSettingsNew,
                keywords = listOf("close", "exit", "quit", "window"),
                action = { it.sendKeyClick(HidConstants.KEY_F4, HidConstants.MOD_LEFT_ALT) }
            ),

            // Virtual Desktops
            PaletteCommand(
                id = "next_desktop",
                title = "Switch to Next Desktop",
                category = "Virtual Desktops",
                shortcutDisplay = "Ctrl + Win + Right",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("desktop", "next", "virtual", "switch", "workspace"),
                action = {
                    it.sendKeyClick(
                        HidConstants.KEY_RIGHT,
                        (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                    )
                }
            ),
            PaletteCommand(
                id = "prev_desktop",
                title = "Switch to Previous Desktop",
                category = "Virtual Desktops",
                shortcutDisplay = "Ctrl + Win + Left",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("desktop", "prev", "previous", "virtual", "switch", "workspace"),
                action = {
                    it.sendKeyClick(
                        HidConstants.KEY_LEFT,
                        (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                    )
                }
            ),
            PaletteCommand(
                id = "new_desktop",
                title = "Create New Virtual Desktop",
                category = "Virtual Desktops",
                shortcutDisplay = "Ctrl + Win + D",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("desktop", "new", "create", "virtual", "add"),
                action = {
                    it.sendKeyClick(
                        HidConstants.KEY_D,
                        (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                    )
                }
            ),
            PaletteCommand(
                id = "close_desktop",
                title = "Close Virtual Desktop",
                category = "Virtual Desktops",
                shortcutDisplay = "Ctrl + Win + F4",
                icon = Icons.Default.DesktopWindows,
                keywords = listOf("desktop", "close", "kill", "remove"),
                action = {
                    it.sendKeyClick(
                        HidConstants.KEY_F4,
                        (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                    )
                }
            ),

            // Developer / Coding
            PaletteCommand(
                id = "vscode_palette",
                title = "VS Code Command Palette",
                category = "Coding",
                shortcutDisplay = "Ctrl + Shift + P",
                icon = Icons.Default.Code,
                keywords = listOf("vscode", "palette", "command", "code"),
                action = {
                    it.sendKeyClick(
                        HidConstants.KEY_P,
                        (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                    )
                }
            ),
            PaletteCommand(
                id = "vscode_file",
                title = "VS Code Quick Open File",
                category = "Coding",
                shortcutDisplay = "Ctrl + P",
                icon = Icons.Default.Code,
                keywords = listOf("vscode", "file", "open", "quick"),
                action = { it.sendKeyClick(HidConstants.KEY_P, HidConstants.MOD_LEFT_CTRL) }
            ),
            PaletteCommand(
                id = "vscode_terminal",
                title = "Toggle Integrated Terminal",
                category = "Coding",
                shortcutDisplay = "Ctrl + `",
                icon = Icons.Default.Code,
                keywords = listOf("terminal", "bash", "shell", "console", "powershell"),
                action = { it.sendKeyClick(HidConstants.KEY_GRAVE, HidConstants.MOD_LEFT_CTRL) }
            ),
            PaletteCommand(
                id = "code_format",
                title = "Format Document",
                category = "Coding",
                shortcutDisplay = "Shift + Alt + F",
                icon = Icons.Default.Code,
                keywords = listOf("format", "prettier", "indent", "clean"),
                action = {
                    it.sendKeyClick(
                        HidConstants.KEY_F,
                        (HidConstants.MOD_LEFT_SHIFT.toInt() or HidConstants.MOD_LEFT_ALT.toInt()).toByte()
                    )
                }
            ),
            PaletteCommand(
                id = "code_comment",
                title = "Toggle Line Comment",
                category = "Coding",
                shortcutDisplay = "Ctrl + /",
                icon = Icons.Default.Code,
                keywords = listOf("comment", "uncomment", "slash"),
                action = { it.sendKeyClick(HidConstants.KEY_SLASH, HidConstants.MOD_LEFT_CTRL) }
            ),
            PaletteCommand(
                id = "devtools",
                title = "Browser / App DevTools",
                category = "Coding",
                shortcutDisplay = "F12",
                icon = Icons.Default.Code,
                keywords = listOf("devtools", "inspect", "console", "browser"),
                action = { it.sendKeyClick(HidConstants.KEY_F12) }
            ),

            // Media
            PaletteCommand(
                id = "media_play_pause",
                title = "Play / Pause Media",
                category = "Media",
                shortcutDisplay = "Media Play/Pause",
                icon = Icons.Default.PlayCircle,
                keywords = listOf("play", "pause", "media", "music", "video"),
                action = { it.sendConsumerClick(HidConstants.CONSUMER_PLAY_PAUSE) }
            ),
            PaletteCommand(
                id = "media_vol_up",
                title = "Volume Up",
                category = "Media",
                shortcutDisplay = "Volume +",
                icon = Icons.Default.PlayCircle,
                keywords = listOf("volume", "up", "sound", "louder"),
                action = { it.sendConsumerClick(HidConstants.CONSUMER_VOLUME_UP) }
            ),
            PaletteCommand(
                id = "media_vol_down",
                title = "Volume Down",
                category = "Media",
                shortcutDisplay = "Volume -",
                icon = Icons.Default.PlayCircle,
                keywords = listOf("volume", "down", "sound", "quieter"),
                action = { it.sendConsumerClick(HidConstants.CONSUMER_VOLUME_DOWN) }
            ),
            PaletteCommand(
                id = "media_mute",
                title = "Mute / Unmute Audio",
                category = "Media",
                shortcutDisplay = "Mute",
                icon = Icons.Default.PlayCircle,
                keywords = listOf("mute", "silence", "audio"),
                action = { it.sendConsumerClick(HidConstants.CONSUMER_MUTE) }
            ),
            PaletteCommand(
                id = "media_next",
                title = "Next Track",
                category = "Media",
                shortcutDisplay = "Next Track",
                icon = Icons.Default.PlayCircle,
                keywords = listOf("next", "track", "song", "forward"),
                action = { it.sendConsumerClick(HidConstants.CONSUMER_SCAN_NEXT) }
            ),
            PaletteCommand(
                id = "media_prev",
                title = "Previous Track",
                category = "Media",
                shortcutDisplay = "Prev Track",
                icon = Icons.Default.PlayCircle,
                keywords = listOf("previous", "prev", "song", "track", "back"),
                action = { it.sendConsumerClick(HidConstants.CONSUMER_SCAN_PREV) }
            ),

            // Presentation
            PaletteCommand(
                id = "present_start",
                title = "Start Slideshow",
                category = "Presentation",
                shortcutDisplay = "F5",
                icon = Icons.Default.Slideshow,
                keywords = listOf("slideshow", "start", "presentation", "powerpoint"),
                action = { it.sendKeyClick(HidConstants.KEY_F5) }
            ),
            PaletteCommand(
                id = "present_current",
                title = "Start from Current Slide",
                category = "Presentation",
                shortcutDisplay = "Shift + F5",
                icon = Icons.Default.Slideshow,
                keywords = listOf("slideshow", "current", "resume"),
                action = { it.sendKeyClick(HidConstants.KEY_F5, HidConstants.MOD_LEFT_SHIFT) }
            ),
            PaletteCommand(
                id = "present_black",
                title = "Blank Screen (Black)",
                category = "Presentation",
                shortcutDisplay = "B",
                icon = Icons.Default.Slideshow,
                keywords = listOf("black", "blank", "dark", "screen"),
                action = { it.sendKeyClick(HidConstants.KEY_B) }
            ),
            PaletteCommand(
                id = "present_white",
                title = "Blank Screen (White)",
                category = "Presentation",
                shortcutDisplay = "W",
                icon = Icons.Default.Slideshow,
                keywords = listOf("white", "blank", "screen"),
                action = { it.sendKeyClick(HidConstants.KEY_W) }
            ),
            PaletteCommand(
                id = "present_exit",
                title = "Exit Slideshow",
                category = "Presentation",
                shortcutDisplay = "Esc",
                icon = Icons.Default.Slideshow,
                keywords = listOf("exit", "stop", "end", "slideshow"),
                action = { it.sendKeyClick(HidConstants.KEY_ESC) }
            )
        )
    }

    val filteredCommands = remember(searchQuery, selectedCategory, allCommands) {
        val q = searchQuery.trim().lowercase()
        allCommands.filter { cmd ->
            val matchesCategory = selectedCategory == "All" || cmd.category == selectedCategory
            val matchesQuery = q.isEmpty() ||
                    cmd.title.lowercase().contains(q) ||
                    cmd.shortcutDisplay.lowercase().contains(q) ||
                    cmd.keywords.any { it.contains(q) }
            matchesCategory && matchesQuery
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(DarkBg)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COMMAND PALETTE",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Instant PC actions, shortcuts & macros",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search actions (e.g. desktop, copy, lock, terminal)…", fontSize = 12.sp, color = TextMuted)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Category Chips (Horizontal Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryBlue else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) PrimaryBlue else DarkBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Command Items List
            if (filteredCommands.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching command found",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCommands, key = { it.id }) { cmd ->
                        CommandItemRow(
                            command = cmd,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                scope.launch {
                                    if (transport != null) {
                                        cmd.action(transport)
                                    }
                                    Toast.makeText(context, "${cmd.title} triggered", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommandItemRow(
    command: PaletteCommand,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = command.icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = command.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = command.category,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }

        // Right Shortcut Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceCard)
                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = command.shortcutDisplay,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = PrimaryBlue
            )
        }
    }
}
