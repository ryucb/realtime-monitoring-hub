package com.ryucb.hub.collector;

import com.ryucb.hub.simulator.SimulatedDevice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SequentialPollerTest {

    private final List<SimulatedDevice> devices = new ArrayList<>();

    @AfterEach
    void tearDown() throws IOException {
        for (SimulatedDevice device : devices) {
            device.close();
        }
    }

    @Test
    void 모든_장비의_값을_읽는다() throws IOException {
        try (Poller poller = new SequentialPoller(startDevices(3, 0))) {
            List<Reading> readings = poller.pollOnce();

            assertThat(readings).extracting(Reading::deviceId).containsExactly(1, 2, 3);
            assertThat(readings).allSatisfy(r -> {
                assertThat(r.temperature()).isBetween(20.0, 26.0);
                assertThat(r.humidity()).isBetween(40.0, 60.0);
                assertThat(r.running()).isTrue();
            });
        }
    }

    @Test
    void 순차_폴링은_장비_수만큼_느려진다() throws IOException {
        long delayMs = 50;
        try (Poller poller = new SequentialPoller(startDevices(4, delayMs))) {
            poller.pollOnce(); // 워밍업

            long start = System.nanoTime();
            poller.pollOnce();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertThat(elapsedMs).isGreaterThanOrEqualTo(4 * delayMs);
        }
    }

    @Test
    void 꺼진_장비는_건너뛰고_나머지는_읽는다() throws IOException {
        List<DeviceConnection> connections = startDevices(2, 0);
        devices.get(1).close();

        try (Poller poller = new SequentialPoller(connections)) {
            assertThat(poller.pollOnce()).extracting(Reading::deviceId).containsExactly(1);
        }
    }

    private List<DeviceConnection> startDevices(int count, long delayMs) throws IOException {
        List<DeviceConnection> connections = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SimulatedDevice device = new SimulatedDevice(i + 1, 0, delayMs);
            device.start();
            devices.add(device);
            connections.add(new DeviceConnection(i + 1, "127.0.0.1", device.port(), 1_000));
        }
        return connections;
    }
}
