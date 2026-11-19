package com.ryucb.hub.modbus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Modbus TCP 프레임을 읽고 쓰는 최소 구현.
 * 이번 프로젝트에서 쓰는 기능 코드는 0x03(Read Holding Registers) 하나다.
 *
 * <pre>
 * MBAP 헤더(7바이트): 트랜잭션 ID(2) | 프로토콜 ID=0(2) | 길이(2) | 유닛 ID(1)
 * 요청 PDU:  기능 코드 0x03(1) | 시작 주소(2) | 레지스터 수(2)
 * 응답 PDU:  기능 코드 0x03(1) | 바이트 수(1) | 레지스터 값(2 x n)
 * </pre>
 */
public final class ModbusTcp {

    public static final int READ_HOLDING_REGISTERS = 0x03;

    private ModbusTcp() {
    }

    /** 클라이언트가 보내는 레지스터 읽기 요청. */
    public record ReadRequest(int transactionId, int unitId, int startAddress, int quantity) {
    }

    public static void writeReadRequest(DataOutputStream out, ReadRequest req) throws IOException {
        out.writeShort(req.transactionId());
        out.writeShort(0);          // 프로토콜 ID
        out.writeShort(6);          // 유닛 ID(1) + PDU(5)
        out.writeByte(req.unitId());
        out.writeByte(READ_HOLDING_REGISTERS);
        out.writeShort(req.startAddress());
        out.writeShort(req.quantity());
        out.flush();
    }

    /** 서버(장비) 쪽에서 요청을 읽는다. 지원하지 않는 기능 코드면 예외. */
    public static ReadRequest readReadRequest(DataInputStream in) throws IOException {
        int transactionId = in.readUnsignedShort();
        in.readUnsignedShort();     // 프로토콜 ID
        in.readUnsignedShort();     // 길이
        int unitId = in.readUnsignedByte();
        int function = in.readUnsignedByte();
        if (function != READ_HOLDING_REGISTERS) {
            throw new IOException("지원하지 않는 기능 코드: " + function);
        }
        int start = in.readUnsignedShort();
        int quantity = in.readUnsignedShort();
        return new ReadRequest(transactionId, unitId, start, quantity);
    }

    public static void writeReadResponse(DataOutputStream out, ReadRequest req, int[] registers) throws IOException {
        int byteCount = registers.length * 2;
        out.writeShort(req.transactionId());
        out.writeShort(0);
        out.writeShort(3 + byteCount);  // 유닛 ID(1) + 기능 코드(1) + 바이트 수(1) + 데이터
        out.writeByte(req.unitId());
        out.writeByte(READ_HOLDING_REGISTERS);
        out.writeByte(byteCount);
        for (int value : registers) {
            out.writeShort(value);
        }
        out.flush();
    }

    /** 클라이언트가 응답을 읽어 레지스터 값 배열로 돌려준다. */
    public static int[] readReadResponse(DataInputStream in, int expectedTransactionId) throws IOException {
        int transactionId = in.readUnsignedShort();
        in.readUnsignedShort();
        in.readUnsignedShort();
        in.readUnsignedByte();
        int function = in.readUnsignedByte();
        if (function != READ_HOLDING_REGISTERS) {
            throw new IOException("예상하지 못한 기능 코드: " + function);
        }
        if (transactionId != expectedTransactionId) {
            throw new IOException("트랜잭션 ID 불일치: " + transactionId + " != " + expectedTransactionId);
        }
        int byteCount = in.readUnsignedByte();
        int[] registers = new int[byteCount / 2];
        for (int i = 0; i < registers.length; i++) {
            registers[i] = in.readUnsignedShort();
        }
        return registers;
    }
}
