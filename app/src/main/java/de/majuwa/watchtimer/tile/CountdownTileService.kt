package de.majuwa.watchtimer.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TypeBuilders
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders
import androidx.wear.protolayout.expression.DynamicDataBuilders
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import de.majuwa.watchtimer.MainActivity
import de.majuwa.watchtimer.service.TimerService
import de.majuwa.watchtimer.timer.TimerStatus
import de.majuwa.watchtimer.timer.TimerUiState
import java.time.Instant

class CountdownTileService : TileService() {
    companion object {
        private const val RESOURCES_VERSION = "1"
        private val END_EPOCH_KEY = AppDataKey<DynamicBuilders.DynamicInstant>("end_epoch_s")
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val timerState = TimerService.uiState.value
        val deviceParams = requestParams.deviceConfiguration
        val tileState = buildTileState(timerState)
        val layout = buildLayout(timerState, deviceParams)
        return Futures.immediateFuture(
            TileBuilders.Tile
                .Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
                .setState(tileState)
                .build(),
        )
    }

    @Suppress("MaxLineLength") // ktlint merges this signature; ProtoLayout type names are unavoidably long
    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(
            ResourceBuilders.Resources
                .Builder()
                .setVersion(RESOURCES_VERSION)
                .build(),
        )

    private fun buildTileState(state: TimerUiState): StateBuilders.State {
        val builder = StateBuilders.State.Builder()
        if (state.status == TimerStatus.RUNNING) {
            val endEpochSec = (System.currentTimeMillis() + state.remainingMs) / 1000L
            builder.addKeyToValueMapping(
                END_EPOCH_KEY,
                DynamicDataBuilders.DynamicDataValue.fromInstant(Instant.ofEpochSecond(endEpochSec)),
            )
        }
        return builder.build()
    }

    private fun buildLayout(
        state: TimerUiState,
        deviceParams: DeviceParametersBuilders.DeviceParameters,
    ): LayoutElementBuilders.LayoutElement {
        val openAction =
            ActionBuilders.LaunchAction
                .Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity
                        .Builder()
                        .setPackageName(packageName)
                        .setClassName(MainActivity::class.java.name)
                        .build(),
                ).build()
        val clickable =
            ModifiersBuilders.Clickable
                .Builder()
                .setId("open")
                .setOnClick(openAction)
                .build()

        val (labelText, timerElement) = buildContent(state)

        val labelElement =
            Text
                .Builder(
                    this,
                    TypeBuilders.StringProp.Builder(labelText).build(),
                    TypeBuilders.StringLayoutConstraint.Builder(labelText).build(),
                ).setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .build()

        return PrimaryLayout
            .Builder(deviceParams)
            .setResponsiveContentInsetEnabled(true)
            .setPrimaryLabelTextContent(labelElement)
            .setContent(timerElement)
            .setPrimaryChipContent(
                CompactChip.Builder(this, "Open", clickable, deviceParams).build(),
            ).build()
    }

    private fun buildContent(state: TimerUiState): Pair<String, LayoutElementBuilders.LayoutElement> =
        when (state.status) {
            TimerStatus.RUNNING -> {
                "Running" to
                    Text
                        .Builder(
                            this,
                            TypeBuilders.StringProp
                                .Builder("--:--")
                                .setDynamicValue(buildDynamicTimerText())
                                .build(),
                            TypeBuilders.StringLayoutConstraint.Builder("00:00").build(),
                        ).setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                        .build()
            }

            TimerStatus.IDLE -> {
                "Tap to open" to
                    Text
                        .Builder(
                            this,
                            TypeBuilders.StringProp.Builder("3:00 · 2:00").build(),
                            TypeBuilders.StringLayoutConstraint.Builder("3:00 · 2:00").build(),
                        ).setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                        .build()
            }

            TimerStatus.PAUSED -> {
                "Paused" to
                    Text
                        .Builder(
                            this,
                            TypeBuilders.StringProp
                                .Builder("%d:%02d".format(state.displayMinutes, state.displaySeconds))
                                .build(),
                            TypeBuilders.StringLayoutConstraint.Builder("00:00").build(),
                        ).setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                        .build()
            }

            TimerStatus.FINISHED -> {
                "Done!" to
                    Text
                        .Builder(
                            this,
                            TypeBuilders.StringProp.Builder("00:00").build(),
                            TypeBuilders.StringLayoutConstraint.Builder("00:00").build(),
                        ).setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                        .build()
            }
        }

    private fun buildDynamicTimerText(): DynamicBuilders.DynamicString {
        val endInstant = DynamicBuilders.DynamicInstant.from(END_EPOCH_KEY)
        val platformTime = DynamicBuilders.DynamicInstant.platformTimeWithSecondsPrecision()
        val remainingDuration = platformTime.durationUntil(endInstant)

        val minutes = remainingDuration.getMinutesPart()
        val seconds = remainingDuration.getSecondsPart()

        val minutesStr = DynamicBuilders.DynamicString.format("%d", minutes)
        val paddedSeconds =
            DynamicBuilders.DynamicString
                .onCondition(seconds.lt(10))
                .use(
                    DynamicBuilders.DynamicString
                        .constant("0")
                        .concat(DynamicBuilders.DynamicString.format("%d", seconds)),
                ).elseUse(DynamicBuilders.DynamicString.format("%d", seconds))

        return minutesStr
            .concat(DynamicBuilders.DynamicString.constant(":"))
            .concat(paddedSeconds)
    }
}
