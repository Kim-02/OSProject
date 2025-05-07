package com.cpu.cpucontroller;

import com.cpu.process.Process;
import com.cpu.process.ProcessTask;
import com.cpu.processor.ProcessorController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MCIQ (Multi-Core Interruptible Queue)
 * 드론 역할별 4개 큐 + static 우선순위 기반 선점 스케줄링
 */
@Component("MCIQ")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CpuSystem_MCIQ extends CpuSystem {

    private static final Map<ProcessTask, Integer> PRIORITY;

    static {
        Map<ProcessTask, Integer> m = new EnumMap<>(ProcessTask.class);
        m.put(ProcessTask.ATTITUDE_ESTIMATION, 12);
        m.put(ProcessTask.ATTITUDE_CONTROL, 11);
        m.put(ProcessTask.RATE_CONTROL, 10);
        m.put(ProcessTask.MOTOR_MIXER, 9);
        m.put(ProcessTask.GPS_PARSER, 8);
        m.put(ProcessTask.BAROMETER_HANDLE, 7);
        m.put(ProcessTask.MAGNETOMETER_HANDLE, 6);
        m.put(ProcessTask.RC_RECEIVER, 5);
        m.put(ProcessTask.AUTO_LANDING, 4);
        m.put(ProcessTask.FAILSAFE_HANDLER, 3);
        m.put(ProcessTask.BATTERY_MONITOR, 2);
        m.put(ProcessTask.TEMPERATURE_MONITOR, 1);
        PRIORITY = Collections.unmodifiableMap(m);
    }

    // 역할별 큐 (flight, sensor, comm, sys)
    private final PriorityQueue<Process> flightQueue = new PriorityQueue<>(
            Comparator.<Process>comparingInt(p -> PRIORITY.get(p.getProcessTask()))
                    .reversed()
                    .thenComparingInt(Process::getArrivalTime)
    );
    private final PriorityQueue<Process> sensorQueue = new PriorityQueue<>(flightQueue.comparator());
    private final PriorityQueue<Process> commQueue = new PriorityQueue<>(flightQueue.comparator());
    private final PriorityQueue<Process> sysQueue = new PriorityQueue<>(flightQueue.comparator());

    public CpuSystem_MCIQ() { // 보류
        super();
    }

    /**
     * MCIQ 전용 프로세스 등록
     */
    public void addMciqProcess(String name,
                               Integer arrivalTime,
                               Integer remainTime,
                               ProcessTask processTask) {
        Process p = Process.builder()
                .ProcessName(name)
                .ArrivalTime(arrivalTime)
                .RemainTime(remainTime)
                .ServiceTime(0)
                .processTask(processTask)
                .build();
        ProcessMap
                .computeIfAbsent(arrivalTime, k -> new LinkedList<>())
                .add(p);
    }

    @Override
    public void setComparatorBasedOnCpu() {
        WaitingProcessQueue = new PriorityQueue<>(Comparator.comparingInt(Process::getArrivalTime));
    }

    @Override
    public void runOneClock() {
        // 1. 도착한 프로세스를 역할 큐로 분배
        if (ProcessMap.containsKey(ProcessingTime)) {
            Queue<Process> arrivals = ProcessMap.get(ProcessingTime);
            while (!arrivals.isEmpty()) {
                Process p = arrivals.poll();
                switch (p.getProcessTask()) {
                    case ATTITUDE_ESTIMATION, ATTITUDE_CONTROL, RATE_CONTROL, MOTOR_MIXER -> flightQueue.add(p);
                    case GPS_PARSER, BAROMETER_HANDLE, MAGNETOMETER_HANDLE -> sensorQueue.add(p);
                    case RC_RECEIVER, AUTO_LANDING -> commQueue.add(p);
                    case FAILSAFE_HANDLER, BATTERY_MONITOR, TEMPERATURE_MONITOR -> sysQueue.add(p);
                    default -> flightQueue.add(p);
                }
            }
        }

        for (ProcessorController proc : ProcessorList) {
            Process run = proc.getUsingProcess();
            if (run != null && run.getRemainTime() <= 0) {
                TerminateProcessQueue.add(proc.RemoveTerminatedProcess(ProcessingTime));
            }
        }
        List<PriorityQueue<Process>> queues = List.of(flightQueue, sensorQueue, commQueue, sysQueue);

        // 2. 선점 + 프로세스 할당
        for (int i = 0; i < ProcessorList.size(); i++) {
            ProcessorController proc = ProcessorList.get(i);
            PriorityQueue<Process> q = queues.get(i);

            Process running = proc.getUsingProcess();
            if (running != null && !q.isEmpty()) {
                Process top = q.peek();
                if (PRIORITY.get(top.getProcessTask()) > PRIORITY.get(running.getProcessTask())) {
                    q.add(proc.PreemptionProcess());
                    proc.setProcess(q.poll());
                }
            }

            if (proc.getUsingProcess() == null && !q.isEmpty()) {
                proc.setProcess(q.poll());
            }
        }

        // 3. 실행 & 전력 소비
        for (ProcessorController proc : ProcessorList) {
            proc.DecreaseUsingProcessBT();
            proc.IncreasePowerConsumption();
        }

        // 6. idle 상태 갱신
        for (ProcessorController proc : ProcessorList) {
            proc.setProcessorStatusNonRunning();
        }

        // 7. 시간 증가
        ProcessingTime++;
    }

    @Override
    protected List<Process> getWaitingProcessList() {
        List<Process> all = new ArrayList<>();
        for (Process p : flightQueue) all.add(copyOf(p));
        for (Process p : sensorQueue) all.add(copyOf(p));
        for (Process p : commQueue) all.add(copyOf(p));
        for (Process p : sysQueue) all.add(copyOf(p));
        return all;
    }

    private Process copyOf(Process p) {
        return Process.builder()
                .ProcessName(p.getProcessName())
                .ArrivalTime(p.getArrivalTime())
                .RemainTime(p.getRemainTime())
                .ServiceTime(p.getServiceTime())
                .TerminateTime(p.getTerminateTime())
                .processTask(p.getProcessTask())
                .build();
    }

    /**
     * 전체 종료 조건
     */
    public boolean isFinished() {
        boolean queuesEmpty =
                flightQueue.isEmpty() &&
                        sensorQueue.isEmpty() &&
                        commQueue.isEmpty() &&
                        sysQueue.isEmpty();

        boolean processorsIdle = ProcessorList.stream()
                .allMatch(p -> p.getUsingProcess() == null);

        boolean processMapEmpty = ProcessMap.values().stream()
                .allMatch(Queue::isEmpty);

        return queuesEmpty && processorsIdle && processMapEmpty;
    }
}