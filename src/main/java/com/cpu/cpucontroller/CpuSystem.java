package com.cpu.cpucontroller;

import com.cpu.dto.ProcessorHistoryDto;
import com.cpu.process.ClockHistory;
import com.cpu.processor.P_Processor;
import lombok.Getter;

import java.util.*;

import com.cpu.process.Process;
import com.cpu.processor.ProcessorController;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public abstract class CpuSystem {
    protected Queue<Process> TerminateProcessQueue = new LinkedList<>();
    protected PriorityQueue<Process> WaitingProcessQueue;
    protected Integer ProcessingTime =0;
    protected ArrayList<ProcessorController> ProcessorList = new ArrayList<>(4);
    protected int ProcessorCount = 0;
    protected Map<Integer,Queue<Process>> ProcessMap = new HashMap<>();
    protected Queue<ClockHistory> ClockHistoryQueue = new LinkedList<>();
    private int timeQuantum = 2; //RR에서 사용하는 timequntum

    public void reset(){
        TerminateProcessQueue.clear();
        WaitingProcessQueue.clear();
        ProcessingTime = 0;
        ProcessorList.clear();
        ProcessorCount = 0;
        ProcessMap.clear();
    }

    public CpuSystem() {
        setComparatorBasedOnCpu();
    }
    public abstract void setComparatorBasedOnCpu();

    public void IncreaseProcessingTime(){
//        log.info("[{}] IncreaseProcessingTime()", this.getClass().getSimpleName());
        ProcessingTime+=1;
    }

    public void setProcessor(ProcessorController p){ //프로세서를 지정해서 0부터 3까지 4개를 배정한다.
        if (ProcessorCount >= 4) {
            throw new IllegalStateException("프로세서는 최대 4개까지만 등록할 수 있습니다.");
        }
        p.setId(ProcessorCount);
        ProcessorList.add(p);
        ProcessorCount++;
    }

    public void setProcess(String ProcessName,Integer AT,Integer BT){

        Process P = Process.builder()
                .ProcessName(ProcessName)
                .ArrivalTime(AT)
                .RemainTime(BT)
                .build();
        int totalProcesses = ProcessMap.values().stream()
                .mapToInt(Queue::size)
                .sum();

        if (totalProcesses >= 15) {
            throw new IllegalStateException("전체 프로세스 수가 15개를 초과했습니다.");
        }

        if (ProcessMap.containsKey(AT)) {
            ProcessMap.get(AT).add(P);
        } else {
            Queue<Process> list = new LinkedList<>();
            list.add(P);
            ProcessMap.put(AT, list);
        }
    }

    public ProcessorController findEmptyProcessor(){
        for (ProcessorController p : ProcessorList) {
            if(p!=null && p.getUsingProcess() == null){
                return p;
            }
        }
        return null;
    }

    //클럭마다 큐를 복사하기 위함
    private List<ProcessorHistoryDto> getProcessorHistoryList() {
        // 1) 리스트 구조부터 복제 (shallow copy된 리스트 객체 생성)
        ArrayList<ProcessorController> controllersCopy = new ArrayList<>(this.ProcessorList);

        // 2) DTO를 담을 리스트 준비 (초기 용량은 복제 리스트 크기)
        List<ProcessorHistoryDto> history = new ArrayList<>(controllersCopy.size());

        // 3) 복제된 리스트를 순회하며, 내부 Process도 깊은 복사 후 DTO 생성
        for (ProcessorController controller : controllersCopy) {
            // a) usingProcess 깊은 복사
            Process raw = controller.getUsingProcess();
            Process usingCopy = null;
            if (raw != null) {
                usingCopy = Process.builder()
                        .ProcessName(raw.getProcessName())
                        .ArrivalTime(raw.getArrivalTime())
                        .RemainTime(raw.getRemainTime())
                        .TerminateTime(raw.getTerminateTime())
                        .build();
            }

            // b) ProcessorHistoryDto 생성 및 추가
            history.add(
                    ProcessorHistoryDto.builder()
                            .id(controller.getId())
                            .usingProcess(usingCopy)
                            .powerConsumption(controller.getPowerConsumption())
                            .isProcessRunning(controller.getIsRunning())
                            .build()
            );
        }

        return history;
    }


    //대기 큐 복사 얕은 복사여도 괜찮음
    private List<Process> getWaitingProcessList(){
        Queue<Process> copy = new LinkedList<>(getWaitingProcessQueue());
        return new ArrayList<>(copy);
    }

    private List<Process> getTerminateProcessList(){
        List<Process> deepCopy = new ArrayList<>();
        for (Process p : getTerminateProcessQueue()) {
            Process pCopy = Process.builder()
                    .ProcessName(p.getProcessName())
                    .ArrivalTime(p.getArrivalTime())
                    .RemainTime(p.getRemainTime())
                    .TerminateTime(p.getTerminateTime())
                    .build();
            deepCopy.add(pCopy);
        }
        return deepCopy;
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


    public abstract void runOneClock();
}
