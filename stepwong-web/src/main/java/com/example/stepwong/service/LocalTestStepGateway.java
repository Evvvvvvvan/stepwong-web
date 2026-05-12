package com.example.stepwong.service;

import com.example.stepwong.dto.StepSubmitResult;
import com.example.stepwong.entity.StepAccount;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local-test")
public class LocalTestStepGateway implements StepGateway {

    @Override
    public StepSubmitResult submit(StepAccount account, String password, int minStep, int maxStep) {
        int step = ThreadLocalRandom.current().nextInt(minStep, maxStep + 1);
        String message = "测试执行完成，已生成步数 " + step + "。";
        return new StepSubmitResult(true, step, message);
    }
}
