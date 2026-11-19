package io.github.ryucb.hub.simulator;

import io.github.ryucb.hub.modbus.ModbusTcp;
import io.github.ryucb.hub.modbus.ModbusTcp.ReadRequest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 실제 장비 대신 쓰는 가상 Modbus TCP 장비.
 *
 * <p>요청을 받으면 {@code responseDelayMs}만큼 기다린 뒤 응답한다.
 * 실제 현장 장비(RS-485 직렬 통신, 변환기 경유 등)의 응답 시간을 흉내 내는 값이다.
 *
 * <p>레지스터 배치:
 * <ul>
 *   <li>0: 온도 x10 (예: 235 = 23.5℃)</li>
 *   <li>1: 습도 x10</li>
 *   <li>2: 운전 상태 (0 정지, 1 운전)</li>
 *   <li>3~6: 응답 시각(epoch ms)을 16비트씩 나눈 값. 화면 반영 지연 측정에 쓴다(3주차).</li>
 * </ul>
 */
public final class SimulatedDevice implements AutoCloseable {

    public static final int REGISTER_COUNT = 7;

    private final int deviceId;
    private final ServerSocket serverSocket;
    private final long responseDelayMs;
    private volatile boolean running = true;

    public SimulatedDevice(int deviceId, int port, long responseDelayMs) throws IOException {
        this.deviceId = deviceId;
        this.responseDelayMs = responseDelayMs;
        this.serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 50);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public int deviceId() {
        return deviceId;
    }

    /** 접속을 받는 스레드를 띄운다. 접속마다 가상 스레드 하나가 요청을 처리한다. */
    public void start() {
        Thread.ofVirtual().name("device-" + deviceId + "-accept").start(() -> {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    Thread.ofVirtual().name("device-" + deviceId + "-conn").start(() -> serve(socket));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[장비 " + deviceId + "] 접속 수락 실패: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void serve(Socket socket) {
        try (socket;
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
            while (running) {
                ReadRequest req = ModbusTcp.readReadRequest(in);
                if (responseDelayMs > 0) {
                    Thread.sleep(responseDelayMs);
                }
                ModbusTcp.writeReadResponse(out, req, slice(currentRegisters(), req.startAddress(), req.quantity()));
            }
        } catch (EOFException | SocketException e) {
            // 클라이언트가 연결을 끊음: 정상 종료
        } catch (IOException e) {
            System.err.println("[장비 " + deviceId + "] 통신 오류: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int[] currentRegisters() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long now = System.currentTimeMillis();
        return new int[]{
                200 + random.nextInt(60),        // 20.0 ~ 25.9℃
                400 + random.nextInt(200),       // 40.0 ~ 59.9%
                1,
                (int) ((now >>> 48) & 0xFFFF),
                (int) ((now >>> 32) & 0xFFFF),
                (int) ((now >>> 16) & 0xFFFF),
                (int) (now & 0xFFFF)
        };
    }

    private static int[] slice(int[] registers, int start, int quantity) {
        int[] result = new int[quantity];
        for (int i = 0; i < quantity; i++) {
            int index = start + i;
            result[i] = index < registers.length ? registers[index] : 0;
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        running = false;
        serverSocket.close();
    }
}
