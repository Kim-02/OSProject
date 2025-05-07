package com.cpu.cpucontroller;

import com.cpu.dto.ProcessResultStatusDto;
import com.cpu.dto.ProcessorHistoryDto;
import com.cpu.process.ClockHistory;
import com.cpu.process.Process;
import com.cpu.processor.ProcessorController;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Getter
@Setter
@Slf4j
public abstract class CpuSystem {
    protected Queue<Process> TerminateProcessQueue = new LinkedList<>();
    protected PriorityQueue<Process> WaitingProcessQueue;
    protected Integer ProcessingTime = 0;
    protected List<ProcessorController> ProcessorList = new ArrayList<>(4);
    protected int ProcessorCount = 0;
    protected Map<Integer, Queue<Process>> ProcessMap = new HashMap<>();
    protected Queue<ClockHistory> ClockHistoryQueue = new LinkedList<>();
    protected Map<String, ProcessResultStatusDto> resultStatusMap = new HashMap<>();

    private int timeQuantum = 2;

    public CpuSystem() {
        setComparatorBasedOnCpu();
    }

    public void reset() {
        TerminateProcessQueue.clear();
        ClockHistoryQueue.clear();
        ProcessMap.clear();
        resultStatusMap.clear();
        ProcessingTime = 0;
        ProcessorList = new ArrayList<>(4);
        ProcessorCount = 0;
        setComparatorBasedOnCpu();
    }

    public abstract void setComparatorBasedOnCpu();

    public void IncreaseProcessingTime() {
        ProcessingTime++;
    }

    public void setProcessor(ProcessorController p) {
        if (ProcessorCount >= 4)
            throw new IllegalStateException("프로세서는 최대 4개까지만 등록할 수 있습니다.");
        p.setId(ProcessorCount);
        ProcessorList.add(p);
        ProcessorCount++;
    }

    public void setProcess(String name, Integer AT, Integer BT) {
        Process p = Process.builder()
                .ProcessName(name)
                .ArrivalTime(AT)
                .RemainTime(BT)
                .ServiceTime(0)
                .build();
        int total = ProcessMap.values().stream().mapToInt(Queue::size).sum();
        if (total >= 15)
            throw new IllegalStateException("전체 프로세스 수가 15개를 초과했습니다.");
        ProcessMap.computeIfAbsent(AT, k -> new LinkedList<>()).add(p);
        resultStatusMap.put(p.getProcessName(),
                ProcessResultStatusDto.builder()
                        .AT(p.getArrivalTime())
                        .BT(p.getRemainTime())
                        .build()
        );
    }

    public ProcessorController findEmptyProcessor() {
        for (ProcessorController p : ProcessorList) {
            if (p != null && p.getUsingProcess() == null) {
                return p;
            }
        }
        return null;
    }

    private List<ProcessorHistoryDto> getProcessorHistoryList() {
        List<ProcessorHistoryDto> history = new ArrayList<>(ProcessorList.size());
        for (ProcessorController c : ProcessorList) {
            Process raw = c.getUsingProcess();
            Process copy = null;
            if (raw != null) {
                copy = Process.builder()
                        .ProcessName(raw.getProcessName())
                        .ArrivalTime(raw.getArrivalTime())
                        .RemainTime(raw.getRemainTime())
                        .ServiceTime(raw.getServiceTime())
                        .processTask(raw.getProcessTask())
                        .TerminateTime(raw.getTerminateTime())
                        .build();
            }
            history.add(
                    ProcessorHistoryDto.builder()
                            .id(c.getId())
                            .usingProcess(copy)
                            .powerConsumption(c.getPowerConsumption())
                            .isProcessRunning(c.getIsRunning())
                            .build()
            );
        }
        return history;
    }

    protected List<Process> getWaitingProcessList() {
        List<Process> snapshot = new ArrayList<>();
        for (Process p : WaitingProcessQueue) {
            snapshot.add(Process.builder()
                    .ProcessName(p.getProcessName())
                    .ArrivalTime(p.getArrivalTime())
                    .RemainTime(p.getRemainTime())
                    .ServiceTime(p.getServiceTime())
                    .processTask(p.getProcessTask())
                    .TerminateTime(p.getTerminateTime())
                    .build()
            );
        }
        return snapshot;
    }

    private List<Process> getTerminateProcessList() {
        List<Process> deep = new ArrayList<>();
        for (Process p : TerminateProcessQueue) {
            deep.add(Process.builder()
                    .ProcessName(p.getProcessName())
                    .ArrivalTime(p.getArrivalTime())
                    .RemainTime(p.getRemainTime())
                    .ServiceTime(p.getServiceTime())
                    .processTask(p.getProcessTask())
                    .TerminateTime(p.getTerminateTime())
                    .build()
            );
        }
        return deep;
    }

    public void SetClockHistory() {

        ClockHistoryQueue.add(
                ClockHistory.builder()
                        .clock(ProcessingTime)
                        .WaitingProcessList(getWaitingProcessList())
                        .ProcessorHistoryList(getProcessorHistoryList())
                        .TerminatedProcessList(getTerminateProcessList())
                        .build()
        );
    }

    public List<ProcessResultStatusDto> getProcessResultStatusList() {
        List<ProcessResultStatusDto> result = new ArrayList<>();
        for (Process p : getTerminateProcessList()) {
            ProcessResultStatusDto dto = resultStatusMap.get(p.getProcessName());
            if (dto != null) {
                dto.setProcessId(p.getProcessName());
                dto.setTT(p.getTerminateTime() - p.getArrivalTime());
                dto.setWT(dto.getTT() - p.getServiceTime());
                dto.setNTT(Math.floor(((double) dto.getTT() / p.getServiceTime()) * 100) / 100.0);
                result.add(dto);
            }
        }
        return result;
    }

    public abstract void runOneClock();
}
