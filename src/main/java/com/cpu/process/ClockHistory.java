package com.cpu.process;

import com.cpu.dto.ProcessorHistoryDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClockHistory {
    private Integer clock;
    private List<Process> WaitingProcessList;
    private List<ProcessorHistoryDto> ProcessorHistoryList;
    private List<Process> TerminatedProcessList;
}
