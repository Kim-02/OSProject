package com.cpu.cpucontroller;

import com.cpu.process.Process;
import com.cpu.processor.ProcessorController;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.PriorityQueue;

@Component("HRRN")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CpuSystem_HRRN extends CpuSystem {

    @Override
    public void setComparatorBasedOnCpu() {
        // Response Ratio = (wait + service) / service 기준으로 내림차순 정렬
        WaitingProcessQueue = new PriorityQueue<>((p1, p2) -> {
            double wait1    = ProcessingTime - p1.getArrivalTime();
            double ratio1   = (wait1 + p1.getRemainTime()) / p1.getRemainTime();
            double wait2    = ProcessingTime - p2.getArrivalTime();
            double ratio2   = (wait2 + p2.getRemainTime()) / p2.getRemainTime();
            // ratio2 - ratio1 순으로 내림차순
            return Double.compare(ratio2, ratio1);
        });
    }

    @Override
    public void runOneClock() {
        // 1) 도착 프로세스 대기 큐에 추가
        if (ProcessMap.containsKey(ProcessingTime)
                && ProcessMap.get(ProcessingTime) != null) {
            while (!ProcessMap.get(ProcessingTime).isEmpty()) {
                WaitingProcessQueue.add(ProcessMap.get(ProcessingTime).poll());
            }
        }

        // 2) 종료된 프로세스 처리
        for (ProcessorController processor : ProcessorList) {
            Process cur = processor.getUsingProcess();
            if (cur != null && cur.getRemainTime() <= 0) {
                Process term = processor.RemoveTerminatedProcess(ProcessingTime);
                TerminateProcessQueue.add(term);
            }
        }

        // 3) 빈 코어가 있으면 HRRN 기준으로 할당 (비선점)
        while (!WaitingProcessQueue.isEmpty() && findEmptyProcessor() != null) {
            ProcessorController free = findEmptyProcessor();
            free.setProcess(WaitingProcessQueue.poll());
        }

        // 4) 비실행 상태 표시
        for (ProcessorController processor : ProcessorList) {
            processor.setProcessorStatusNonRunning();
        }

        // 5) 실행 및 전력 소모 처리
        for (ProcessorController processor : ProcessorList) {
            processor.DecreaseUsingProcessBT();
            processor.IncreasePowerConsumption();
        }
    }
}

