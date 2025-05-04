package com.cpu.cpucontroller;

import com.cpu.process.Process;
import com.cpu.process.ProcessTask;
import com.cpu.processor.ProcessorController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import java.util.*;

@Slf4j
@Component("MCIQ")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CpuSystem_MCIQ extends CpuSystem {

    // 우선순위 정의
    private final Map<ProcessTask, Integer> taskPriority = Map.ofEntries(
            Map.entry(ProcessTask.ATTITUDE_ESTIMATION, 12),
            Map.entry(ProcessTask.ATTITUDE_CONTROL, 11),
            Map.entry(ProcessTask.RATE_CONTROL, 10),
            Map.entry(ProcessTask.MOTOR_MIXER, 9),
            Map.entry(ProcessTask.GPS_PARSER, 8),
            Map.entry(ProcessTask.BAROMETER_HANDLE, 7),
            Map.entry(ProcessTask.MAGNETOMETER_HANDLE, 6),
            Map.entry(ProcessTask.RC_RECEIVER, 5),
            Map.entry(ProcessTask.AUTO_LANDING, 4),
            Map.entry(ProcessTask.FAILSAFE_HANDLER, 3),
            Map.entry(ProcessTask.BATTERY_MONITOR, 2),
            Map.entry(ProcessTask.TEMPERATURE_MONITOR, 1)
    );


    // 코어별 우선순위 큐 (코어당 1개씩만 사용)
    private final Map<ProcessorController, PriorityQueue<Process>> coreQueues = new HashMap<>();

    @Override
    public void setComparatorBasedOnCpu() {
        Comparator<Process> comparator = Comparator
                .comparingInt((Process p) -> taskPriority.getOrDefault(p.getProcessTask(), 0)).reversed()
                .thenComparingInt(Process::getArrivalTime);
        for (ProcessorController processor : ProcessorList) {
            coreQueues.put(processor, new PriorityQueue<>(comparator));
        }
    }

    @Override
    public void runOneClock() {
        // 1. 도착한 프로세스를 적절한 코어 큐에 분배
        if (ProcessMap.containsKey(ProcessingTime)) {
            while (!ProcessMap.get(ProcessingTime).isEmpty()) {
                Process p = ProcessMap.get(ProcessingTime).poll();
                ProcessTask task = p.getProcessTask();

                int coreIndex = getCoreIndexForTask(task);
                if (coreIndex >= 0 && coreIndex < ProcessorList.size()) {
                    ProcessorController processor = ProcessorList.get(coreIndex);
                    coreQueues.get(processor).add(p);
                }
            }
        }

        // 2. 종료된 프로세스 처리
        for (ProcessorController processor : ProcessorList) {
            Process current = processor.getUsingProcess();
            if (current != null && current.getRemainTime() <= 0) {
                TerminateProcessQueue.add(processor.RemoveTerminatedProcess(ProcessingTime));
            }
        }

        // 3. 선점 포함 - 우선순위 높은 프로세스를 실행
        for (ProcessorController processor : ProcessorList) {
            PriorityQueue<Process> queue = coreQueues.get(processor);
            Process current = processor.getUsingProcess();

            if (!queue.isEmpty()) {
                Process candidate = queue.peek();

                if (current == null) {
                    // 현재 실행 중인 프로세스가 없으면 바로 실행
                    processor.setProcess(queue.poll());
                } else {
                    int currentPriority = taskPriority.getOrDefault(current.getProcessTask(), 0);
                    int candidatePriority = taskPriority.getOrDefault(candidate.getProcessTask(), 0);

                    if (candidatePriority > currentPriority || (
                            candidatePriority == currentPriority &&
                                    candidate.getArrivalTime() < current.getArrivalTime())) {
                        // 선점 조건 충족
                        queue.add(current); // 현재 실행 중인 프로세스를 다시 큐에 넣음
                        processor.setProcess(queue.poll()); // 새로 실행할 프로세스 선택
                    }
                }
            }
        }

        // 4. 실행 중인 프로세스 시간 감소
        for (ProcessorController processor : ProcessorList) {
            processor.DecreaseUsingProcessBT();
        }
    }


    // 작업에 따라 코어를 분배
    private int getCoreIndexForTask(ProcessTask task) {
        return switch (task) {
            case ATTITUDE_ESTIMATION, ATTITUDE_CONTROL, RATE_CONTROL, MOTOR_MIXER -> 0;
            case GPS_PARSER, BAROMETER_HANDLE, MAGNETOMETER_HANDLE -> 1;
            case RC_RECEIVER, AUTO_LANDING -> 2;
            case FAILSAFE_HANDLER, BATTERY_MONITOR, TEMPERATURE_MONITOR -> 3;
        };
    }
}
