package com.timelord.common.network.message;

public final class TimeShiftMessages {
    private TimeShiftMessages() {}

    public record State(boolean active, int multiplier) {}

    public record StartCharge() {}

    public record Release() {}

    public record Burst() {}
}
