package com.example.stepwong.service;

import com.example.stepwong.dto.StepSubmitResult;
import com.example.stepwong.entity.StepAccount;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("!local-test")
public class ZeppLifeStepGateway implements StepGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZeppLifeStepGateway.class);

    private static final String APP_NAME = "com.xiaomi.hm.health";
    private static final String APP_VERSION = "6.14.0";
    private static final String APP_SOURCE = "com.xiaomi.hm.health:6.14.0:50818";
    private static final String DEVICE_TYPE = "0";
    private static final String DEFAULT_DEVICE_ID = "DA932FFFFE8816E7";
    private static final String LOGIN_AES_KEY = "xeNtBVqzDc6tuNTh";
    private static final String LOGIN_AES_IV = "MAAAYAAAAAAAAABg";
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("(?<=access=).*?(?=&|$)");

    private final ObjectMapper objectMapper;
    private final CryptoService cryptoService;
    private final int timeoutSeconds;
    private final boolean trustAllSsl;

    public ZeppLifeStepGateway(
            ObjectMapper objectMapper,
            CryptoService cryptoService,
            @Value("${app.zepp.timeout-seconds:15}") int timeoutSeconds,
            @Value("${app.zepp.ssl-trust-all:false}") boolean trustAllSsl
    ) {
        this.objectMapper = objectMapper;
        this.cryptoService = cryptoService;
        this.timeoutSeconds = timeoutSeconds;
        this.trustAllSsl = trustAllSsl;
    }

    @Override
    public StepSubmitResult submit(StepAccount account, String password, int minStep, int maxStep) {
        List<String> trace = new ArrayList<>();
        int step = ThreadLocalRandom.current().nextInt(minStep, maxStep + 1);
        try {
            addTrace(account, trace, "开始执行，随机步数=" + step + "，范围=" + minStep + "~" + maxStep);
            TokenState tokenState = resolveToken(account, password, true, trace);
            UploadResult uploadResult = uploadStep(account, tokenState, step, trace);
            if (!uploadResult.success() && tokenState.fromCache()) {
                addTrace(account, trace, "缓存 token 上报失败，清空缓存后重新登录重试");
                clearTokenCache(account);
                tokenState = resolveToken(account, password, false, trace);
                uploadResult = uploadStep(account, tokenState, step, trace);
            }
            if (uploadResult.success()) {
                addTrace(account, trace, "执行成功，Zepp 接口已返回 code=1/message=success");
                logFullTrace(account, trace);
                return new StepSubmitResult(true, step, "修改步数成功（" + step + "）");
            }
            addTrace(account, trace, "执行失败，Zepp 返回未通过成功判定：" + uploadResult.message());
            logFullTrace(account, trace);
            return new StepSubmitResult(false, step, uploadResult.message());
        } catch (Exception e) {
            addTrace(account, trace, "执行异常：" + e.getMessage());
            logFullTrace(account, trace);
            return new StepSubmitResult(false, step, toUserMessage(e));
        }
    }

    private TokenState resolveToken(StepAccount account, String password, boolean allowCache, List<String> trace) throws Exception {
        if (allowCache) {
            TokenState cached = readTokenCache(account);
            if (cached.usable()) {
                addTrace(account, trace, "命中 token 缓存，loginToken=存在，appToken=" + (StringUtils.hasText(cached.appToken()) ? "存在" : "缺失"));
                if (!StringUtils.hasText(cached.appToken())) {
                    addTrace(account, trace, "缓存缺少 appToken，开始获取 appToken");
                    cached = cached.withAppToken(fetchAppToken(account, cached.loginToken(), trace));
                    cacheToken(account, cached);
                    addTrace(account, trace, "appToken 获取完成并写入缓存");
                }
                return cached;
            }
            addTrace(account, trace, "未命中可用 token 缓存，开始完整登录");
        } else {
            addTrace(account, trace, "跳过缓存，开始完整登录");
        }
        TokenState tokenState = login(account, password, trace);
        addTrace(account, trace, "开始获取 CN appToken，避免复用登录响应中的非 CN token");
        tokenState = tokenState.withAppToken(fetchAppToken(account, tokenState.loginToken(), trace));
        cacheToken(account, tokenState);
        addTrace(account, trace, "登录 token 已写入缓存");
        return tokenState;
    }

    private TokenState readTokenCache(StepAccount account) {
        String loginToken = decryptNullable(account.getEncryptedLoginToken());
        String appToken = decryptNullable(account.getEncryptedAppToken());
        String userId = account.getZeppUserId();
        String deviceId = account.getZeppDeviceId();
        return new TokenState(loginToken, appToken, userId, normalizeDeviceId(deviceId), true);
    }

    private void cacheToken(StepAccount account, TokenState tokenState) {
        Instant now = Instant.now();
        if (StringUtils.hasText(tokenState.loginToken())) {
            account.setEncryptedLoginToken(cryptoService.encrypt(tokenState.loginToken()));
            account.setLoginTokenUpdatedAt(now);
        }
        if (StringUtils.hasText(tokenState.appToken())) {
            account.setEncryptedAppToken(cryptoService.encrypt(tokenState.appToken()));
            account.setAppTokenUpdatedAt(now);
        }
        if (StringUtils.hasText(tokenState.userId())) {
            account.setZeppUserId(tokenState.userId());
        }
        if (StringUtils.hasText(tokenState.deviceId())) {
            account.setZeppDeviceId(tokenState.deviceId());
        }
    }

    private void clearTokenCache(StepAccount account) {
        account.setEncryptedLoginToken(null);
        account.setEncryptedAppToken(null);
        account.setZeppUserId(null);
        account.setLoginTokenUpdatedAt(null);
        account.setAppTokenUpdatedAt(null);
    }

    private TokenState login(StepAccount account, String password, List<String> trace) throws Exception {
        String user = normalizeUser(account.getAccountNo());
        String deviceId = normalizeDeviceId(account.getZeppDeviceId());
        boolean phone = user.contains("+86");
        addTrace(account, trace, "登录第一步开始，账号类型=" + (phone ? "手机号" : "邮箱"));
        String code = fetchAccessCode(account, user, password, trace);
        if (!StringUtils.hasText(code)) {
            throw new IllegalStateException("登录失败：未获取授权码，请检查账号密码或 Zepp 是否风控");
        }
        Map<String, String> form = new LinkedHashMap<>();
        if (phone) {
            form.put("app_name", APP_NAME);
            form.put("app_version", APP_VERSION);
            form.put("code", code);
            form.put("country_code", "CN");
            form.put("device_id", deviceId);
            form.put("device_model", "phone");
            form.put("grant_type", "access_token");
            form.put("third_name", "huami_phone");
        } else {
            form.put("allow_registration", "false");
            form.put("app_name", APP_NAME);
            form.put("app_version", APP_VERSION);
            form.put("code", code);
            form.put("country_code", "CN");
            form.put("device_id", deviceId);
            form.put("device_model", "android_phone");
            form.put("dn", "account.zepp.com%2Capi-user.zepp.com%2Capi-mifit.zepp.com%2Capi-watch.zepp.com%2Capp-analytics.zepp.com%2Capi-analytics.huami.com%2Cauth.zepp.com");
            form.put("grant_type", "access_token");
            form.put("lang", "zh_CN");
            form.put("os_version", "1.5.0");
            form.put("source", APP_SOURCE);
            form.put("third_name", "email");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("app_name", APP_NAME);
        headers.put("x-request-id", UUID.randomUUID().toString());
        headers.put("accept-language", "zh-CN");
        headers.put("appname", APP_NAME);
        headers.put("cv", "50818_6.14.0");
        headers.put("v", "2.0");
        headers.put("appplatform", "android_phone");
        headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
        addTrace(account, trace, "登录第二步开始，使用 access code 换取 login_token");
        HttpResponseData response = postString("https://account.huami.com/v2/client/login", headers, formEncode(form));
        addTrace(account, trace, "登录第二步响应，HTTP=" + response.statusCode() + "，body=" + limit(response.body(), 160));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("登录失败：获取 login_token 失败");
        }
        JsonNode tokenInfo = requireTokenInfo(response.body(), "获取 login_token 失败");
        String loginToken = requiredText(tokenInfo, "login_token", "login_token 缺失");
        String userId = requiredText(tokenInfo, "user_id", "user_id 缺失");
        String appToken = textOrNull(tokenInfo, "app_token");
        addTrace(account, trace, "登录第二步成功，userid=" + maskValue(userId) + "，响应 appToken=" + (StringUtils.hasText(appToken) ? "存在但不用于 CN 上报" : "缺失"));
        return new TokenState(loginToken, null, userId, deviceId, false);
    }

    private String fetchAccessCode(StepAccount account, String user, String password, List<String> trace) throws Exception {
        Map<String, String> loginData = new LinkedHashMap<>();
        loginData.put("emailOrPhone", user);
        loginData.put("password", password);
        loginData.put("state", "REDIRECTION");
        loginData.put("client_id", "HuaMi");
        loginData.put("country_code", "CN");
        loginData.put("token", "access");
        loginData.put("redirect_uri", "https://s3-us-west-2.amazonaws.com/hm-registration/successsignin.html");
        byte[] encryptedBody = encryptLoginData(formEncode(loginData).getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("user-agent", "MiFit6.14.0 (M2007J1SC; Android 12; Density/2.75)");
        headers.put("app_name", APP_NAME);
        headers.put("appname", APP_NAME);
        headers.put("appplatform", "android_phone");
        headers.put("x-hm-ekv", "1");
        headers.put("hm-privacy-ceip", "false");
        HttpResponseData response = postBytes("https://api-user.zepp.com/v2/registrations/tokens", headers, encryptedBody);
        addTrace(account, trace, "登录第一步响应，HTTP=" + response.statusCode() + "，Location=" + limit(response.header("Location"), 160) + "，body=" + limit(response.body(), 120));
        if (response.statusCode() != 303) {
            throw new IllegalStateException("登录失败：账号或密码错误，或 Zepp 拒绝登录");
        }
        String location = response.header("Location");
        if (!StringUtils.hasText(location)) {
            throw new IllegalStateException("登录失败：Zepp 未返回授权跳转地址");
        }
        Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(location);
        if (!matcher.find()) {
            throw new IllegalStateException("登录失败：未获取授权码，请检查账号密码或 Zepp 是否风控");
        }
        String code = URLDecoder.decode(matcher.group(), "UTF-8");
        addTrace(account, trace, "登录第一步成功，access code 已解析");
        return code;
    }

    private String fetchAppToken(StepAccount account, String loginToken, List<String> trace) throws Exception {
        String url = "https://account-cn.huami.com/v1/client/app_tokens?app_name=" + APP_NAME
                + "&dn=api-user.huami.com%2Capi-mifit.huami.com%2Capp-analytics.huami.com&login_token="
                + encode(loginToken);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "MiFit/5.3.0 (iPhone; iOS 14.7.1; Scale/3.00)");
        addTrace(account, trace, "获取 appToken 开始");
        HttpResponseData response = get(url, headers);
        addTrace(account, trace, "获取 appToken 响应，HTTP=" + response.statusCode() + "，body=" + limit(response.body(), 160));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("token失效：获取 app_token 失败");
        }
        JsonNode tokenInfo = requireTokenInfo(response.body(), "获取 app_token 失败");
        String appToken = requiredText(tokenInfo, "app_token", "app_token 缺失");
        addTrace(account, trace, "获取 appToken 成功");
        return appToken;
    }

    private UploadResult uploadStep(StepAccount account, TokenState tokenState, int step, List<String> trace) throws Exception {
        String t = String.valueOf(Instant.now().toEpochMilli());
        String date = LocalDate.now().toString();
        String dataJson = buildMimotionBandDataJson(date, step, DEFAULT_DEVICE_ID);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("apptoken", tokenState.appToken());
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("User-Agent", "python-requests/2.31.0");
        String requestBody = buildMimotionUploadBody(tokenState.userId(), dataJson);
        addTrace(account, trace, "步数上报开始，userid=" + maskValue(tokenState.userId()) + "，date=" + date + "，step=" + step + "，dataJson长度=" + dataJson.length() + "，请求体长度=" + requestBody.length() + "，流程=mimotion，deviceType=0，lastSync=1597306380，lastDeviceId=" + DEFAULT_DEVICE_ID);
        HttpResponseData response = postString("https://api-mifit-cn.huami.com/v1/data/band_data.json?&t=" + t + "&r=" + UUID.randomUUID().toString(), headers, requestBody);
        addTrace(account, trace, "步数上报响应，域名=cn，HTTP=" + response.statusCode() + "，body=" + limit(response.body(), 200));
        if (response.statusCode() != 200) {
            return new UploadResult(false, friendlyUploadHttpMessage(response.statusCode()));
        }
        String message = parseResponseMessage(response.body());
        boolean success = isSuccessfulResponse(response.body(), message);
        if (!success) {
            return new UploadResult(false, friendlyUploadBodyMessage(response.body(), message));
        }
        return new UploadResult(true, "success");
    }

    private String buildMimotionUploadBody(String userId, String dataJson) {
        StringBuilder body = new StringBuilder();
        body.append("userid=").append(encode(userId));
        body.append("&last_sync_data_time=1597306380");
        body.append("&device_type=0");
        body.append("&last_deviceid=").append(DEFAULT_DEVICE_ID);
        body.append("&data_json=").append(encode(dataJson));
        return body.toString();
    }

    private String buildMimotionBandDataJson(String date, int step, String deviceId) throws Exception {
        Map<String, Object> sleep = new LinkedHashMap<>();
        sleep.put("ss", 73);
        sleep.put("lt", 304);
        sleep.put("dt", 0);
        sleep.put("st", 1589920140);
        sleep.put("lb", 36);
        sleep.put("dp", 92);
        sleep.put("is", 208);
        sleep.put("rhr", 0);
        sleep.put("stage", buildSleepStageData());
        sleep.put("ed", 1589943900);
        sleep.put("wk", 0);
        sleep.put("wc", 0);

        Map<String, Object> stepSummary = new LinkedHashMap<>();
        stepSummary.put("runCal", 1);
        stepSummary.put("cal", 6);
        stepSummary.put("conAct", 0);
        stepSummary.put("stage", new ArrayList<Object>());
        stepSummary.put("ttl", step);
        stepSummary.put("dis", 144);
        stepSummary.put("rn", 0);
        stepSummary.put("wk", 5);
        stepSummary.put("runDist", 4);
        stepSummary.put("ncal", 0);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("slp", sleep);
        summary.put("tz", "28800");
        summary.put("stp", stepSummary);
        summary.put("v", 5);
        summary.put("goal", 8000);

        Map<String, Object> dataItem = new LinkedHashMap<>();
        dataItem.put("stop", 1439);
        dataItem.put("value", buildMimotionMinuteData());
        dataItem.put("did", deviceId);
        dataItem.put("tz", 32);
        dataItem.put("src", 17);
        dataItem.put("start", 0);

        Map<String, Object> summaryHeartRate = new LinkedHashMap<>();
        summaryHeartRate.put("ct", 0);
        summaryHeartRate.put("id", new ArrayList<Object>());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", objectMapper.writeValueAsString(summary));
        payload.put("data", Collections.singletonList(dataItem));
        payload.put("data_hr", buildMimotionHeartRateData());
        payload.put("summary_hr", objectMapper.writeValueAsString(summaryHeartRate));
        payload.put("date", date);
        return objectMapper.writeValueAsString(Collections.singletonList(payload));
    }

    private List<Map<String, Object>> buildSleepStageData() {
        int[][] values = new int[][]{
                {269, 357, 2},
                {358, 380, 3},
                {381, 407, 2},
                {408, 423, 3},
                {424, 488, 2},
                {489, 502, 3},
                {503, 512, 2},
                {513, 522, 3},
                {523, 568, 2},
                {569, 581, 3},
                {582, 638, 2},
                {639, 654, 3},
                {655, 665, 2}
        };
        List<Map<String, Object>> stages = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            Map<String, Object> stage = new LinkedHashMap<>();
            stage.put("start", values[i][0]);
            stage.put("stop", values[i][1]);
            stage.put("mode", values[i][2]);
            stages.add(stage);
        }
        return stages;
    }

    private String buildMimotionMinuteData() {
        return repeat("AU", 2880);
    }

    private String buildMimotionHeartRateData() {
        return repeat("/v7+", 480);
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private byte[] encryptLoginData(byte[] plainBytes) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(LOGIN_AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(LOGIN_AES_IV.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
        return cipher.doFinal(plainBytes);
    }

    private JsonNode requireTokenInfo(String body, String errorPrefix) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode tokenInfo = root.path("token_info");
        if (tokenInfo.isMissingNode() || tokenInfo.isNull()) {
            throw new IllegalStateException(errorPrefix + "，响应缺少 token_info");
        }
        return tokenInfo;
    }

    private String parseResponseMessage(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String message = textOrNull(root, "message");
            if (StringUtils.hasText(message)) {
                return message;
            }
            String msg = textOrNull(root, "msg");
            if (StringUtils.hasText(msg)) {
                return msg;
            }
            return body;
        } catch (Exception e) {
            return body;
        }
    }

    private boolean isSuccessfulResponse(String body, String message) {
        String normalized = message == null ? "" : message.trim();
        if (!"success".equalsIgnoreCase(normalized)) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode code = root.path("code");
            if (code.isNumber()) {
                return code.asInt() == 1;
            }
            if (code.isTextual()) {
                return "1".equals(code.asText());
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void addTrace(StepAccount account, List<String> trace, String message) {
        String safeMessage = sanitizeLog(message);
        trace.add(safeMessage);
        LOGGER.info("ZeppLife流程 account={} {}", maskValue(account.getAccountNo()), safeMessage);
    }

    private String buildTraceMessage(List<String> trace) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < trace.size(); i++) {
            if (i > 0) {
                builder.append(" -> ");
            }
            builder.append(trace.get(i));
        }
        return limit(builder.toString(), 1400);
    }

    private String sanitizeLog(String message) {
        if (message == null) {
            return "";
        }
        String sanitized = message.replaceAll("(?i)(login_token|app_token|apptoken|access_token|access)=([^,&\\s}]+)", "$1=***");
        return sanitized.replaceAll("(?i)(\\\"(?:login_token|app_token|apptoken|access_token|access)\\\"\\s*:\\s*\\\")[^\\\"]+(\\\")", "$1***$2");
    }

    private String maskValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String text = value.trim();
        if (text.length() <= 4) {
            return "****";
        }
        return "****" + text.substring(text.length() - 4);
    }

    private void logFullTrace(StepAccount account, List<String> trace) {
        LOGGER.info("ZeppLife流程 account={} 完整链路：{}", maskValue(account.getAccountNo()), buildTraceMessage(trace));
    }

    private String friendlyUploadHttpMessage(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return "token失效：Zepp 拒绝上报，请重新登录后再试";
        }
        if (statusCode >= 500) {
            return "步数上报失败：Zepp 服务端拒绝处理上报数据";
        }
        return "步数上报失败：Zepp 返回 HTTP " + statusCode;
    }

    private String friendlyUploadBodyMessage(String body, String message) {
        if (StringUtils.hasText(message) && !body.equals(message)) {
            String normalized = message.toLowerCase();
            if (normalized.contains("token") || normalized.contains("auth") || normalized.contains("unauthorized")) {
                return "token失效：Zepp 上报鉴权失败";
            }
        }
        return "步数上报失败：Zepp 未返回成功结果";
    }

    private String toUserMessage(Exception e) {
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            return "执行失败：未知异常";
        }
        if (message.contains("SSLHandshakeException") || message.contains("PKIX")) {
            return "网络证书异常：JDK 证书库不信任 Zepp 接口";
        }
        if (message.contains("Connection timed out") || message.contains("Read timed out") || message.contains("timeout")) {
            return "网络异常：请求 Zepp 接口超时";
        }
        if (message.startsWith("登录失败") || message.startsWith("token失效")) {
            return message;
        }
        if (message.contains("login_token")) {
            return "登录失败：账号密码校验未通过";
        }
        if (message.contains("app_token")) {
            return "token失效：获取 app_token 失败";
        }
        return "执行失败：" + limit(message, 120);
    }

    private String requiredText(JsonNode node, String field, String errorMessage) {
        String value = textOrNull(node, field);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(errorMessage);
        }
        return value;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private String decryptNullable(String encryptedText) {
        if (!StringUtils.hasText(encryptedText)) {
            return null;
        }
        try {
            return cryptoService.decrypt(encryptedText);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeUser(String accountNo) {
        String user = accountNo == null ? "" : accountNo.trim();
        if (user.contains("+86") || user.contains("@")) {
            return user;
        }
        return "+86" + user;
    }

    private String normalizeDeviceId(String deviceId) {
        if (StringUtils.hasText(deviceId)) {
            return deviceId;
        }
        return UUID.randomUUID().toString();
    }

    private String formEncode(Map<String, String> form) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            parts.add(encode(entry.getKey()) + "=" + encode(entry.getValue()));
        }
        return String.join("&", parts);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException("URL 编码失败", e);
        }
    }

    private String limit(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private HttpResponseData get(String url, Map<String, String> headers) throws Exception {
        return request("GET", url, headers, null);
    }

    private HttpResponseData postString(String url, Map<String, String> headers, String body) throws Exception {
        return request("POST", url, headers, body.getBytes(StandardCharsets.UTF_8));
    }

    private HttpResponseData postBytes(String url, Map<String, String> headers, byte[] body) throws Exception {
        return request("POST", url, headers, body);
    }

    private HttpResponseData request(String method, String url, Map<String, String> headers, byte[] body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        if (trustAllSsl && connection instanceof HttpsURLConnection) {
            configureTrustAllSsl((HttpsURLConnection) connection);
        }
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setConnectTimeout(timeoutSeconds * 1000);
        connection.setReadTimeout(timeoutSeconds * 1000);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Length", String.valueOf(body.length));
            OutputStream outputStream = connection.getOutputStream();
            try {
                outputStream.write(body);
            } finally {
                outputStream.close();
            }
        }
        int statusCode = connection.getResponseCode();
        String responseBody = readResponseBody(connection);
        return new HttpResponseData(statusCode, responseBody, connection.getHeaderField("Location"));
    }


    private void configureTrustAllSsl(HttpsURLConnection connection) {
        connection.setSSLSocketFactory(TrustAllSslHolder.SOCKET_FACTORY);
        connection.setHostnameVerifier(TrustAllSslHolder.HOSTNAME_VERIFIER);
    }

    private String readResponseBody(HttpURLConnection connection) throws Exception {
        InputStream inputStream = connection.getErrorStream();
        if (inputStream == null) {
            try {
                inputStream = connection.getInputStream();
            } catch (Exception e) {
                return "";
            }
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            inputStream.close();
        }
    }


    private static class TrustAllSslHolder {

        private static final SSLSocketFactory SOCKET_FACTORY = createSocketFactory();
        private static final HostnameVerifier HOSTNAME_VERIFIER = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        private static SSLSocketFactory createSocketFactory() {
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, new SecureRandom());
                return context.getSocketFactory();
            } catch (Exception e) {
                throw new IllegalStateException("初始化 Zepp SSL 兼容配置失败", e);
            }
        }
    }

    private static class HttpResponseData {

        private final int statusCode;
        private final String body;
        private final String location;

        private HttpResponseData(int statusCode, String body, String location) {
            this.statusCode = statusCode;
            this.body = body;
            this.location = location;
        }

        private int statusCode() {
            return statusCode;
        }

        private String body() {
            return body;
        }

        private String header(String name) {
            if ("Location".equalsIgnoreCase(name)) {
                return location;
            }
            return null;
        }
    }

    private static class TokenState {

        private final String loginToken;
        private final String appToken;
        private final String userId;
        private final String deviceId;
        private final boolean fromCache;

        private TokenState(String loginToken, String appToken, String userId, String deviceId, boolean fromCache) {
            this.loginToken = loginToken;
            this.appToken = appToken;
            this.userId = userId;
            this.deviceId = deviceId;
            this.fromCache = fromCache;
        }

        private String loginToken() {
            return loginToken;
        }

        private String appToken() {
            return appToken;
        }

        private String userId() {
            return userId;
        }

        private String deviceId() {
            return deviceId;
        }

        private boolean fromCache() {
            return fromCache;
        }

        private boolean usable() {
            return StringUtils.hasText(loginToken) && StringUtils.hasText(userId) && StringUtils.hasText(deviceId);
        }

        private TokenState withAppToken(String appToken) {
            return new TokenState(loginToken, appToken, userId, deviceId, fromCache);
        }
    }

    private static class UploadResult {

        private final boolean success;
        private final String message;

        private UploadResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        private boolean success() {
            return success;
        }

        private String message() {
            return message;
        }
    }
}
