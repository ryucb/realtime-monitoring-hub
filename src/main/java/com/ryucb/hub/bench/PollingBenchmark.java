package com.ryucb.hub.bench;

import com.ryucb.hub.collector.DeviceConnection;
import com.ryucb.hub.collector.Poller;
import com.ryucb.hub.collector.Reading;
import com.ryucb.hub.collector.SequentialPoller;
import com.ryucb.hub.simulator.SimulatedDevice;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 장비 수를 늘려 가며 폴링 한 바퀴에 걸리는 시간을 잰다.
 *
 * <pre>
 * 사용법: PollingBenchmark [응답 지연 ms=100] [측정 바퀴 수=5] [장비 수 목록=1,5,15,30,50]
 * 결과:   콘솔 표 + results/benchmark-&lt;방식&gt;-&lt;지연&gt;ms.csv
 * </pre>
 */
public final class PollingBenchmark {

    private static final int TIMEOUT_MS = 5_000;

    public static void main(String[] args) throws Exception {
        long delayMs = args.length > 0 ? Long.parseLong(args[0]) : 100;
        int rounds = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int[] deviceCounts = args.length > 2
                ? Arrays.stream(args[2].split(",")).mapToInt(Integer::parseInt).toArray()
                : new int[]{1, 5, 15, 30, 50};

        System.out.printf("장비 응답 지연 %dms, 장비 수별 %d바퀴 측정%n%n", delayMs, rounds);
        System.out.printf("%-12s %8s %16s %16s %10s%n", "방식", "장비 수", "평균 한 바퀴(ms)", "최대 한 바퀴(ms)", "읽은 값");

        List<String> csv = new ArrayList<>();
        csv.add("poller,devices,device_delay_ms,avg_cycle_ms,max_cycle_ms,readings_per_cycle");

        for (int count : deviceCounts) {
            List<SimulatedDevice> devices = startDevices(count, delayMs);
            try (Poller poller = new SequentialPoller(connect(devices))) {
                poller.pollOnce(); // 워밍업: 첫 바퀴는 JIT·접속 비용이 섞여 제외

                long total = 0;
                long max = 0;
                int readingsPerCycle = 0;
                for (int r = 0; r < rounds; r++) {
                    long start = System.nanoTime();
                    List<Reading> readings = poller.pollOnce();
                    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                    total += elapsedMs;
                    max = Math.max(max, elapsedMs);
                    readingsPerCycle = readings.size();
                }
                long avg = total / rounds;
                System.out.printf("%-12s %8d %16d %16d %10d%n", poller.name(), count, avg, max, readingsPerCycle);
                csv.add(String.join(",", poller.name(), String.valueOf(count), String.valueOf(delayMs),
                        String.valueOf(avg), String.valueOf(max), String.valueOf(readingsPerCycle)));
            } finally {
                for (SimulatedDevice device : devices) {
                    device.close();
                }
            }
        }

        Path out = Path.of("results", "benchmark-sequential-" + delayMs + "ms.csv");
        Files.createDirectories(out.getParent());
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out))) {
            csv.forEach(writer::println);
        }
        System.out.printf("%n결과 저장: %s%n", out);
    }

    private static List<SimulatedDevice> startDevices(int count, long delayMs) throws IOException {
        List<SimulatedDevice> devices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // 포트 0 = 운영체제가 빈 포트를 골라 준다 (측정을 반복해도 포트 충돌이 없음)
            SimulatedDevice device = new SimulatedDevice(i + 1, 0, delayMs);
            device.start();
            devices.add(device);
        }
        return devices;
    }

    private static List<DeviceConnection> connect(List<SimulatedDevice> devices) throws IOException {
        List<DeviceConnection> connections = new ArrayList<>(devices.size());
        for (SimulatedDevice device : devices) {
            connections.add(new DeviceConnection(device.deviceId(), "127.0.0.1", device.port(), TIMEOUT_MS));
        }
        return connections;
    }
}
