package com.cpu.processor;


import com.cpu.process.Process;
import org.springframework.stereotype.Component;

@Component
public interface ProcessorController {
    void IncreasePowerConsumption();
    void DecreaseUsingProcessBT();
    void setProcess(Process usingProcess);
    Process getUsingProcess();
    Process RemoveTerminatedProcess(Integer currentTime);
    void setProcessorStatusNonRunning();
    Double getPowerConsumption();
    Process PreemptionProcess();
    void setId(Integer id);
    Integer getId();
    Boolean getIsRunning();
}
