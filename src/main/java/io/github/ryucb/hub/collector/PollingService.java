package io.github.ryucb.hub.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 정해진 주기로 모든 장비를 한 바퀴 읽고, 장비별 최신 값과 마지막 한 바퀴 시간을 보관한다.
 * 3주차에 읽은 값을 Kafka로 보내는 부분이 여기에 붙는다.
 */
@Service
public class PollingService {

    private static final Logger log = LoggerFactory.getLogger(PollingService.class);

    private final Poller poller;
    private final Map<Integer, Reading> latest = new ConcurrentHashMap<>();
    private volatile CycleStats lastCycle = CycleStats.EMPTY;

    public PollingService(Poller poller) {
        this.poller = poller;
    }

    /** 한 바퀴가 끝난 뒤 interval-ms만큼 쉬고 다음 바퀴를 돈다. */
    @Scheduled(fixedDelayString = "${hub.polling.interval-ms}")
    public void poll() {
        long start = System.nanoTime();
        List<Reading> readings = poller.pollOnce();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        readings.forEach(r -> latest.put(r.deviceId(), r));
        lastCycle = new CycleStats(poller.name(), poller.deviceCount(), readings.size(), elapsedMs,
                System.currentTimeMillis());
        log.info("[{}] 장비 {}대 중 {}대 읽음, 한 바퀴 {}ms", poller.name(), poller.deviceCount(),
                readings.size(), elapsedMs);
    }

    public List<Reading> latestReadings() {
        return latest.values().stream()
                .sorted(Comparator.comparingInt(Reading::deviceId))
                .toList();
    }

    public CycleStats lastCycle() {
        return lastCycle;
    }

    /**
     * 마지막 한 바퀴의 요약.
     *
     * @param poller      폴링 방식
     * @param devices     대상 장비 수
     * @param succeeded   읽기에 성공한 장비 수
     * @param cycleMs     한 바퀴에 걸린 시간
     * @param finishedAt  한 바퀴가 끝난 시각(epoch ms)
     */
    public record CycleStats(String poller, int devices, int succeeded, long cycleMs, long finishedAt) {
        static final CycleStats EMPTY = new CycleStats("-", 0, 0, 0, 0);
    }
}
