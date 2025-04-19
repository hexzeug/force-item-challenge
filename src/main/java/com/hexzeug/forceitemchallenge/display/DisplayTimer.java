package com.hexzeug.forceitemchallenge.display;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DisplayTimer {
    private final int seconds;
    private final int minutes;
    private final int hours;

    public DisplayTimer(long time) {
        long nonNegativeTime = Math.max(0, time);
        long secondsLeft = nonNegativeTime / 20;
        long minutesLeft = secondsLeft / 60;
        long hoursLeft = minutesLeft / 60;
        this.seconds = (int) (secondsLeft % 60);
        this.minutes = (int) (minutesLeft % 60);
        this.hours = (int) hoursLeft;
    }

    public Text toText() {
        return toFormattedText(Formatting.RESET).copy().fillStyle(Style.EMPTY);
    }

    public Text toFormattedText(Formatting number, Formatting... global) {
        MutableText start;
        if (hours > 0) {
            start = Text.empty()
                    .append(Text
                            .literal(Integer.toString(hours))
                            .formatted(number)
                    )
                    .append(":");
        } else {
            start = Text.empty();
        }
        String paddedSeconds = (seconds < 10) ? "0" + seconds : Integer.toString(seconds);
        String paddedMinutes = (minutes < 10) ? "0" + minutes : Integer.toString(minutes);
        return start
                .append(Text
                        .literal(paddedMinutes)
                        .formatted(number)
                )
                .append(":")
                .append(Text
                        .literal(paddedSeconds)
                        .formatted(number)
                )
                .formatted(global);
    }
}
