package io.github.ryucb.hub.collector;

import java.util.List;

/** 연결된 모든 장비를 한 바퀴 읽는 방식. 1주차는 순차, 2주차에 병렬 구현을 추가한다. */
public interface Poller extends AutoCloseable {

    /** 모든 장비를 한 번씩 읽고 결과를 돌려준다. 실패한 장비는 결과에서 빠진다. */
    List<Reading> pollOnce();

    String name();

    @Override
    void close();
}
