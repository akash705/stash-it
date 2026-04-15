package com.stashed.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.stashed.app.MainActivity
import com.stashed.app.R

/**
 * Home screen widget — 4×1 or 2×1 quick-save input.
 *
 * The widget shows a "Stash something" prompt and an Open button.
 * Tapping "Open" launches the main app's save screen.
 *
 * Uses Glance (Jetpack Compose for widgets) with PreferencesGlanceStateDefinition
 * to store the last saved text.
 */
class StashedWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val lastSaved = prefs[StashedWidgetKeys.LAST_SAVED] ?: ""

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .background(R.color.widget_background),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "📦 Stash something",
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text),
                    ),
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )

                Button(
                    text = "Open",
                    onClick = actionStartActivity<MainActivity>(),
                )
            }

            if (lastSaved.isNotBlank()) {
                Text(
                    text = "Last: $lastSaved",
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text_secondary),
                    ),
                    modifier = GlanceModifier.padding(start = 8.dp, top = 4.dp),
                )
            }
        }
    }
}

private object StashedWidgetKeys {
    val LAST_SAVED = stringPreferencesKey("last_saved")
}

/**
 * BroadcastReceiver that tells Android about the widget.
 * Registered in AndroidManifest.xml.
 */
class StashedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StashedWidget()
}
