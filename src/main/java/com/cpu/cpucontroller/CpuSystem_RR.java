package com.cpu.cpucontroller;

import com.cpu.process.Process;
import com.cpu.processor.ProcessorController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
@Slf4j
@Component("RR")
public class CpuSystem_RR extends CpuSystem {

    private final Map<ProcessorController, Integer> quantumCounter = new HashMap<>();

    @Override
    public void setComparatorBasedOnCpu() {
        WaitingProcessQueue = new PriorityQueue<>(
                Comparator.comparingInt(Process::getArrivalTime)
        );
    }

    @Override
    public void runOneClock() {
        // 1) 새로 도착한 프로세스 대기 큐에 추가
        if (ProcessMap.containsKey(ProcessingTime)) {
            Queue<Process> arrivals = ProcessMap.get(ProcessingTime);
            while (arrivals != null && !arrivals.isEmpty()) {
                WaitingProcessQueue.add(arrivals.poll());
            }
        }

        // 2) 완료된 프로세스 제거
        for (ProcessorController proc : ProcessorList) {
            Process cur = proc.getUsingProcess();
            if (cur != null && cur.getRemainTime() <= 0) {
                Process terminated = proc.RemoveTerminatedProcess(ProcessingTime);
                TerminateProcessQueue.add(terminated);
                quantumCounter.remove(proc);
            }
        }

        boolean allBusy = (findEmptyProcessor() == null);
        // 3-1) 모든 코어 바쁨
        // 3-2) 선점 가능 후보
        List<ProcessorController> candidates = new ArrayList<>();
        for (ProcessorController proc : ProcessorList) {
            Process cur = proc.getUsingProcess();
            if (cur != null &&
                    quantumCounter.getOrDefault(proc, 0) >= getTimeQuantum()) {
                candidates.add(proc);
            }
        }
        // 3-3) 대기 큐에 남은 프로세스가 있는지
        boolean hasWaiting = !WaitingProcessQueue.isEmpty();

        if (allBusy && !candidates.isEmpty() && hasWaiting) {
            // 실제 선점할 개수 = 대기 큐 크기만큼만
            int num = Math.min(candidates.size(), WaitingProcessQueue.size());
            List<ProcessorController> toPreempt = candidates.subList(0, num);

            // 1) 피해자(victim)를 모아두고 카운터 리셋
            List<Process> victims = new ArrayList<>();
            for (ProcessorController proc : toPreempt) {
                Process v = proc.PreemptionProcess();
                victims.add(v);
                quantumCounter.put(proc, 0);
//                log.info("[선점] t={} 코어{} 프로세스 {} 선점", ProcessingTime, proc.getId(), v.getProcessName());
            }

            // 2) 빈 코어 대신 위에서 선점된 코어에 바로 새 프로세스 할당
            for (ProcessorController proc : toPreempt) {
                Process next = WaitingProcessQueue.poll();
                proc.setProcess(next);
                quantumCounter.put(proc, 0);
//                log.info("[할당] t={} 코어{} 프로세스 {} 할당 (선점 후)",
//                        ProcessingTime, proc.getId(), next.getProcessName());
            }

            // 3) 이제 피해자들을 대기 큐 뒤에 추가 (이 주기에는 재할당 안됨)
            for (Process v : victims) {
                WaitingProcessQueue.add(v);
//                log.info("[대기열] t={} 프로세스 {} 대기열 뒤로 이동",
//                        ProcessingTime, v.getProcessName());
            }
        } else {
            // 선점 조건 아니면 기존처럼 빈 코어 우선 할당
            while (!WaitingProcessQueue.isEmpty() && findEmptyProcessor() != null) {
                ProcessorController free = findEmptyProcessor();
                Process next = WaitingProcessQueue.poll();
                free.setProcess(next);
                quantumCounter.put(free, 0);
//                log.info("[할당] t={} 코어{} 프로세스 {} 할당 (빈 코어)",
//                        ProcessingTime, free.getId(), next.getProcessName());
            }
        }

        // 5) 비실행 상태인 코어 표시
        for (ProcessorController proc : ProcessorList) {
            proc.setProcessorStatusNonRunning();
        }

        // 6) 실행: BT 감소, 전력 증가, 퀀텀 카운터 증가
        for (ProcessorController proc : ProcessorList) {
            proc.DecreaseUsingProcessBT();
            proc.IncreasePowerConsumption();
            if (proc.getUsingProcess() != null) {
                quantumCounter.merge(proc, 1, Integer::sum);
            }
        }
    }


}