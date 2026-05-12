package com.example.stepwong.dto;

public class StepSubmitResult {

    private final boolean success;
    private final int stepCount;
    private final String message;

    public StepSubmitResult(boolean success, int stepCount, String message) {
        this.success = success;
        this.stepCount = stepCount;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStepCount() {
        return stepCount;
    }

    public String getMessage() {
        return message;
    }
}
