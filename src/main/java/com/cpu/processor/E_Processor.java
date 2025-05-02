package com.cpu.processor;

import com.cpu.process.Process;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;


@Component
@Getter
@Setter
public class E_Processor implements ProcessorController{
    public Integer Id;
    private Process usingProcess;
    private Double powerConsumption = 0.0;
    private Boolean isProcessorRunning = false;
    private final Integer WorkingCounter = 1;
    @Override
    public void IncreasePowerConsumption() {
        if(usingProcess != null) {
            if(!isProcessorRunning) {
                powerConsumption += 1.1;
                isProcessorRunning = true;
            }
            else{
                powerConsumption += 1.0;
            }
        }
    }
    @Override
    public void DecreaseUsingProcessBT(){
        if(usingProcess != null) { // 만약 처리할 수 있는 프로세스라면
            usingProcess.setRemainTime(usingProcess.getRemainTime()-WorkingCounter); //  시간을 1 감소시킨다
        }
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
    public Process RemoveTerminatedProcess(Integer currentTime){
        Process process = usingProcess;
        process.setTerminateTime(currentTime);
        usingProcess = null;
        return process;
    }

    @Override
    public void setProcessorStatusNonRunning(){
        if(usingProcess == null && isProcessorRunning){
            isProcessorRunning = false;
        }
    }
    @Override
    public Boolean getIsRunning(){
        return isProcessorRunning;
    }

}
