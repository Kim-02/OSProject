package com.cpu.cpucontroller;

import com.cpu.processor.ProcessorController;
import com.cpu.process.Process;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.PriorityQueue;

@Component("SPN")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CpuSystem_SPN extends CpuSystem {

    @Override
    public void setComparatorBasedOnCpu() {
        // PriorityQueue를 사용하여 자동으로 정렬된 상태로 프로세스를 저장합니다.
        WaitingProcessQueue = new PriorityQueue<>(Comparator.comparingInt(Process::getRemainTime));
    }

    @Override
    public void runOneClock() {
        // 도착 시간에 맞는 프로세스가 대기 큐에 추가되는 부분
        if (ProcessMap.containsKey(ProcessingTime) && ProcessMap.get(ProcessingTime) != null) {
            while (!ProcessMap.get(ProcessingTime).isEmpty()) {
                WaitingProcessQueue.add(ProcessMap.get(ProcessingTime).poll());
            }
        }

        // 각 프로세서를 확인하고, 종료된 프로세스는 종료 큐에 추가
        for (ProcessorController processor : ProcessorList) {
            if (processor.getUsingProcess() != null && processor.getUsingProcess().getRemainTime() <= 0) {
                Process terminatedProcess = processor.RemoveTerminatedProcess(ProcessingTime);
                TerminateProcessQueue.add(terminatedProcess);
            }
        }

        // 대기 큐에서 빈 프로세서에 프로세스를 할당
        while (!WaitingProcessQueue.isEmpty() && findEmptyProcessor() != null) {
            ProcessorController availableProcessor = findEmptyProcessor();
            availableProcessor.setProcess(WaitingProcessQueue.poll());
        }

        // 프로세서가 사용 중이지 않으면 비활성화
        for (ProcessorController processor : ProcessorList) {
            processor.setProcessorStatusNonRunning();
        }

        // 프로세서가 작업을 처리하고 전력을 소모하며, 버스트 타임을 감소시킴
        for (ProcessorController processor : ProcessorList) {
            processor.DecreaseUsingProcessBT();
            processor.IncreasePowerConsumption();
        }
    }

}
