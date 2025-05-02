package com.cpu.dto;

import com.cpu.process.Process;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessorHistoryDto {
    private Integer id;
    private Process usingProcess;
    private Double powerConsumption;
    private Boolean isProcessRunning;
}
