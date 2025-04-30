package com.cpu.cpucontroller;

import com.cpu.Processor.P_Processor;
import lombok.Getter;

import java.util.*;

import com.cpu.process.Process;
import com.cpu.Processor.ProcessorController;

@Getter
public abstract class CpuSystem {
    protected Queue<Process> TerminateProcessQueue = new LinkedList<>();
    protected PriorityQueue<Process> WaitingProcessQueue;
    protected Integer ProcessingTime =0;
    protected ProcessorController[] ProcessorList = new ProcessorController[4];
    protected int ProcessorCount = 0;
    protected Map<Integer,Queue<Process>> ProcessMap = new HashMap<>();

    public CpuSystem() {
        setComparatorBasedOnCpu();
    }
    public abstract void setComparatorBasedOnCpu();

    public void IncreaseProcessingTime(){
        ProcessingTime+=1;
    }

    public void setProcessor(ProcessorController p){ //프로세서를 지정해서 0부터 3까지 4개를 배정한다.
        if (ProcessorCount >= 4) {
            throw new IllegalStateException("프로세서는 최대 4개까지만 등록할 수 있습니다.");
        }
        ProcessorList[ProcessorCount] = p;
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
            if(p.getUsingProcess() == null){
                return p;
            }
        }
        return null;
    }


    public void printProcessorStatus() {
        System.out.println("\n=== 클럭 " + ProcessingTime + " ===");
        for (int i = 0; i < ProcessorList.length; i++) {
            ProcessorController processor = ProcessorList[i];
            String processorType = (processor instanceof P_Processor) ? "P코어" : "E코어";
            String processInfo;
            if (processor.getUsingProcess() != null) {
                processInfo = "실행중: " + processor.getUsingProcess().getProcessName()
                        + " | 남은시간: " + processor.getUsingProcess().getRemainTime();
            } else {
                processInfo = "실행중: 비어있음 | 남은시간: -" ;
            }
            System.out.println("[" + i + "번 프로세서] 타입: " + processorType
                    + " | " + processInfo
                    + " | 소비전력: " + processor.getPowerConsumption());
        }
    }

    public abstract void runOneClock();
}
