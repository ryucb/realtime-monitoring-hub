package com.ryucb.hub.collector;

import com.ryucb.hub.modbus.ModbusTcp;
import com.ryucb.hub.modbus.ModbusTcp.ReadRequest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 장비 하나와의 Modbus TCP 연결.
 *
 * <p>처음 읽을 때 접속하고, 이후에는 연결을 재사용한다. 통신 중 오류가 나면 연결을 닫고,
 * 다음 읽기에서 다시 접속한다. 장비가 꺼져 있다가 다시 켜져도 수집기를 재시작할 필요가 없다.
 */
public final class DeviceConnection implements AutoCloseable {

    private final int deviceId;
    private final String host;
    private final int port;
    private final int timeoutMs;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private int nextTransactionId = 1;

    public DeviceConnection(int deviceId, String host, int port, int timeoutMs) {
        this.deviceId = deviceId;
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    public int deviceId() {
        return deviceId;
    }

    public String address() {
        return host + ":" + port;
    }

    /** 레지스터를 읽는다. 응답이 올 때까지(또는 타임아웃까지) 호출한 스레드가 기다린다. */
    public int[] readRegisters(int startAddress, int quantity) throws IOException {
        ensureConnected();
        int transactionId = nextTransactionId;
        nextTransactionId = (nextTransactionId % 0xFFFF) + 1;
        try {
            ModbusTcp.writeReadRequest(out, new ReadRequest(transactionId, 1, startAddress, quantity));
            return ModbusTcp.readReadResponse(in, transactionId);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    private void ensureConnected() throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            return;
        }
        Socket newSocket = new Socket();
        newSocket.connect(new InetSocketAddress(host, port), timeoutMs);
        newSocket.setSoTimeout(timeoutMs);
        newSocket.setTcpNoDelay(true);
        socket = newSocket;
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    @Override
    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // 닫는 중 오류는 무시
            }
            socket = null;
        }
    }
}
