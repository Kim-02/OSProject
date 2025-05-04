package com.cpu.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessResultStatusDto {
    private String processId;
    private Integer AT;
    private Integer BT;
    private Integer WT;
    private Integer TT;
    private Double NTT;
}
