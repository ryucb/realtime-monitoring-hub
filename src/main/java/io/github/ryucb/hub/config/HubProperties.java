package io.github.ryucb.hub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 {@code hub.*} 설정.
 *
 * @param devices 수집 대상 장비
 * @param polling 폴링 주기와 타임아웃
 */
@ConfigurationProperties(prefix = "hub")
public record HubProperties(Devices devices, Polling polling) {

    /**
     * @param host     장비 주소 (가상 장비는 127.0.0.1)
     * @param basePort 첫 장비의 포트. 장비 N번은 basePort + N - 1
     * @param count    장비 수
     */
    public record Devices(String host, int basePort, int count) {
    }

    /**
     * @param intervalMs 한 바퀴가 끝난 뒤 다음 바퀴까지 쉬는 시간
     * @param timeoutMs  장비 하나의 접속·응답 제한 시간
     */
    public record Polling(long intervalMs, int timeoutMs) {
    }
}
