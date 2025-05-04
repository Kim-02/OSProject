package com.cpu.process;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SchedulingRequest {
    private String algorithm;
    private List<Process> processList;
    private List<String> processorList;
    private Integer TimeQuantum;
    private ProcessTask processTask;
}
