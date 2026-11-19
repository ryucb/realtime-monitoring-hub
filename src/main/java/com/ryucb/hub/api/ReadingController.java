package com.ryucb.hub.api;

import com.ryucb.hub.collector.PollingService;
import com.ryucb.hub.collector.PollingService.CycleStats;
import com.ryucb.hub.collector.Reading;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 수집 상태를 확인하는 API. 3주차에 WebSocket 실시간 푸시가 추가된다. */
@RestController
@RequestMapping("/api")
public class ReadingController {

    private final PollingService pollingService;

    public ReadingController(PollingService pollingService) {
        this.pollingService = pollingService;
    }

    /** 장비별 최신 값. */
    @GetMapping("/readings")
    public List<Reading> readings() {
        return pollingService.latestReadings();
    }

    /** 마지막 한 바퀴의 폴링 방식, 장비 수, 성공 수, 소요 시간. */
    @GetMapping("/status")
    public CycleStats status() {
        return pollingService.lastCycle();
    }
}
