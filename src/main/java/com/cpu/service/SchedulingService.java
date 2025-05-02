package com.cpu.service;

import com.cpu.ResponseJson;
import com.cpu.cpucontroller.CpuSystem;
import com.cpu.process.Process;
import com.cpu.process.SchedulingRequest;
import com.cpu.processor.E_Processor;
import com.cpu.processor.P_Processor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingService {
    private final Map<String, CpuSystem> cpuSystemMap;

    public ResponseEntity<ResponseJson<Object>> run(SchedulingRequest request) {
        // ① 요청된 알고리즘 키
        log.info(">>> 요청 알고리즘 키: {}", request.getAlgorithm());
        // ② 맵에 등록된 모든 키
        log.info(">>> 사용 가능한 알고리즘 키: {}", cpuSystemMap.keySet());
        CpuSystem system = cpuSystemMap.get(request.getAlgorithm().toUpperCase());
        log.info(">>> 선택된 CpuSystem Bean: {}",
                system != null ? system.getClass().getSimpleName() : "null");
        if (system == null) {
            return ResponseEntity.notFound().build();
        }
        system.reset();
        if(request.getTimeQuantum()!=null){
            system.setTimeQuantum(request.getTimeQuantum());
        }
        // 프로세스 등록
        for (Process process : request.getProcessList()) {
            system.setProcess(
                    process.getProcessName(),
                    process.getArrivalTime(),
                    process.getRemainTime()
            );
        }

        // 코어 타입 설정
        for (String coreType : request.getProcessorList()) {
            if ("P".equalsIgnoreCase(coreType)) {
                system.setProcessor(new P_Processor());
            } else if ("E".equalsIgnoreCase(coreType)) {
                system.setProcessor(new E_Processor());
            } else {
                return ResponseEntity.ok(
                        ResponseJson.builder()
                                .name("schedule")
                                .message("Unknown processor type: " + coreType)
                                .build()
                );
            }
        }
        system.setProcessorCount(request.getProcessorList().size());

        // 시뮬레이션 수행
        int totalProcesses = request.getProcessList().size();
        while (system.getTerminateProcessQueue().size() < totalProcesses) {
            system.runOneClock();
            system.IncreaseProcessingTime();
            system.SetClockHistory();
        }
        return ResponseEntity.ok(
                ResponseJson.builder()
                        .name("schedule success")
                        .message("update ClockPoint")
                        .data(system.getClockHistoryQueue())
                        .build()

        );
    }
}
