package com.cpu.process;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Process {
    private String ProcessName;
    private Integer ArrivalTime;
    private Integer RemainTime;
    private Integer TerminateTime; // 프로세스가 몇 초에 끝났는지
    private ProcessTask ProcessTask;     // 작업 유형 (enum)
}
