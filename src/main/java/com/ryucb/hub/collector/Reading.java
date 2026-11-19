package com.ryucb.hub.collector;

/**
 * 장비 한 대에서 한 번 읽은 값.
 *
 * @param deviceId         장비 번호
 * @param temperature      온도(℃)
 * @param humidity         습도(%)
 * @param running          운전 중 여부
 * @param deviceTimeMs     장비가 응답을 만든 시각(epoch ms)
 * @param collectedAtMs    수집기가 응답을 받은 시각(epoch ms)
 */
public record Reading(int deviceId, double temperature, double humidity, boolean running,
                      long deviceTimeMs, long collectedAtMs) {

    public static Reading fromRegisters(int deviceId, int[] registers, long collectedAtMs) {
        long deviceTime = ((long) registers[3] << 48) | ((long) registers[4] << 32)
                | ((long) registers[5] << 16) | registers[6];
        return new Reading(deviceId, registers[0] / 10.0, registers[1] / 10.0, registers[2] == 1,
                deviceTime, collectedAtMs);
    }
}
