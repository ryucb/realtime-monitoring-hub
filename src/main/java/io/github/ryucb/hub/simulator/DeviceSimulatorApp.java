package io.github.ryucb.hub.simulator;

import java.util.ArrayList;
import java.util.List;

/**
 * 가상 장비 여러 대를 띄워 두는 실행 프로그램.
 *
 * <pre>
 * 사용법: DeviceSimulatorApp [장비 수=15] [시작 포트=15020] [응답 지연 ms=100]
 * 예:     DeviceSimulatorApp 15 15020 100  → 15020~15034 포트에 장비 15대
 * </pre>
 */
public final class DeviceSimulatorApp {

    public static void main(String[] args) throws Exception {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 15;
        int basePort = args.length > 1 ? Integer.parseInt(args[1]) : 15020;
        long delayMs = args.length > 2 ? Long.parseLong(args[2]) : 100;

        List<SimulatedDevice> devices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SimulatedDevice device = new SimulatedDevice(i + 1, basePort + i, delayMs);
            device.start();
            devices.add(device);
        }
        System.out.printf("가상 장비 %d대 실행 중 (포트 %d~%d, 응답 지연 %dms). 종료: Ctrl+C%n",
                count, basePort, basePort + count - 1, delayMs);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (SimulatedDevice device : devices) {
                try {
                    device.close();
                } catch (Exception ignored) {
                    // 종료 중이라 무시
                }
            }
        }));
        Thread.currentThread().join();
    }
}
