package com.cpu.processor;

import com.cpu.process.Process;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class P_Processor implements ProcessorController{
    private Process usingProcess;
    private Double powerConsumption = 0.0;
    private Boolean isProcessorRunning = false;
    private final Integer WorkingCounter = 2;
    @Override
    public void IncreasePowerConsumption() {
        if(usingProcess != null) {
            if(!isProcessorRunning) {
                powerConsumption += 3.5;
                isProcessorRunning = true;
            }
            else{
                powerConsumption += 3.0;
            }
        }
    }
    @Override
    public void DecreaseUsingProcessBT(){
        if(usingProcess != null) { // 만약 처리할 수 있는 프로세스라면
            usingProcess.setRemainTime(usingProcess.getRemainTime()-WorkingCounter); //  시간을 2 감소시킨다
        }
    }
    public Process RemoveTerminatedProcess(Integer currentTime){
        Process process = usingProcess;
        process.setTerminateTime(currentTime);
        usingProcess = null;
        return process;
    }
    @Override
    public void setProcess(Process usingProcess) {
        this.usingProcess = usingProcess;
    }

    @Override
    public Process PreemptionProcess() {
        Process process = usingProcess;
        usingProcess = null;
        return process;
    }

    @Override
    public void setProcessorStatusNonRunning(){
        if(usingProcess == null && isProcessorRunning){
            isProcessorRunning = false;
        }
    }

}
