package com.example.stepwong.service;

import com.example.stepwong.dto.StepSubmitResult;
import com.example.stepwong.entity.StepAccount;

public interface StepGateway {

    StepSubmitResult submit(StepAccount account, String password, int minStep, int maxStep);
}
