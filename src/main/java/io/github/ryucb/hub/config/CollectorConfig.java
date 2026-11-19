package io.github.ryucb.hub.config;

import io.github.ryucb.hub.collector.DeviceConnection;
import io.github.ryucb.hub.collector.Poller;
import io.github.ryucb.hub.collector.SequentialPoller;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/** 설정값으로 장비 연결과 폴링 방식을 만든다. 2주차에 병렬 폴링을 고를 수 있게 확장한다. */
@Configuration
public class CollectorConfig {

    @Bean(destroyMethod = "close")
    public Poller poller(HubProperties properties) {
        HubProperties.Devices devices = properties.devices();
        List<DeviceConnection> connections = new ArrayList<>(devices.count());
        for (int i = 0; i < devices.count(); i++) {
            connections.add(new DeviceConnection(i + 1, devices.host(), devices.basePort() + i,
                    properties.polling().timeoutMs()));
        }
        return new SequentialPoller(connections);
    }
}
