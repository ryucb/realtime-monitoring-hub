package io.github.ryucb.hub.collector;

import io.github.ryucb.hub.modbus.ModbusTcp;
import io.github.ryucb.hub.modbus.ModbusTcp.ReadRequest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/** 장비 하나와 맺어 두는 Modbus TCP 연결. 매 폴링마다 다시 접속하지 않고 재사용한다. */
public final class DeviceConnection implements AutoCloseable {

    private final int deviceId;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private int nextTransactionId = 1;

    public DeviceConnection(int deviceId, String host, int port, int timeoutMs) throws IOException {
        this.deviceId = deviceId;
        this.socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);
        socket.setSoTimeout(timeoutMs);
        socket.setTcpNoDelay(true);
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public int deviceId() {
        return deviceId;
    }

    /** 레지스터를 읽는다. 응답이 올 때까지(또는 타임아웃까지) 호출한 스레드가 기다린다. */
    public int[] readRegisters(int startAddress, int quantity) throws IOException {
        int transactionId = nextTransactionId;
        nextTransactionId = (nextTransactionId % 0xFFFF) + 1;
        ModbusTcp.writeReadRequest(out, new ReadRequest(transactionId, 1, startAddress, quantity));
        return ModbusTcp.readReadResponse(in, transactionId);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
