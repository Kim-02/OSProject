package com.cpu.dto;

import com.cpu.process.Process;
import com.cpu.process.ProcessTask;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessorHistoryDto implements Serializable {
    private Integer id;
    private Process usingProcess;
    private String processName;
    private Integer arrivalTime;
    private Integer remainTime;
    private Integer serviceTime;
    private ProcessTask processTask;
    private Double powerConsumption;
    private Boolean isProcessRunning;
}
