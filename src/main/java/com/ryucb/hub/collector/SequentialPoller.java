package com.ryucb.hub.collector;

import com.ryucb.hub.simulator.SimulatedDevice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 승일 미들웨어의 구조를 재현한 단일 스레드 순차 폴링.
 *
 * <p>스레드 하나가 장비를 1번부터 차례로 읽는다. 한 장비의 응답을 기다리는 동안
 * 다음 장비는 아무것도 하지 못하므로, 한 바퀴 시간은 대략 (장비 수 x 장비 응답 시간)이 된다.
 * 장비가 하나라도 늦거나 응답이 없으면 그 뒤의 모든 장비가 함께 늦어진다.
 */
public final class SequentialPoller implements Poller {

    private final List<DeviceConnection> connections;

    public SequentialPoller(List<DeviceConnection> connections) {
        this.connections = List.copyOf(connections);
    }

    @Override
    public List<Reading> pollOnce() {
        List<Reading> readings = new ArrayList<>(connections.size());
        for (DeviceConnection connection : connections) {
            try {
                int[] registers = connection.readRegisters(0, SimulatedDevice.REGISTER_COUNT);
                readings.add(Reading.fromRegisters(connection.deviceId(), registers, System.currentTimeMillis()));
            } catch (IOException e) {
                System.err.println("[순차 폴링] 장비 " + connection.deviceId() + "(" + connection.address()
                        + ") 읽기 실패: " + e.getMessage());
            }
        }
        return readings;
    }

    @Override
    public int deviceCount() {
        return connections.size();
    }

    @Override
    public String name() {
        return "sequential";
    }

    @Override
    public void close() {
        connections.forEach(DeviceConnection::close);
    }
}
