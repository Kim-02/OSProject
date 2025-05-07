package com.cpu.service;

import com.cpu.ResponseJson;
import com.cpu.cpucontroller.CpuSystem;
import com.cpu.cpucontroller.CpuSystem_MCIQ;
import com.cpu.dto.ProcessResultStatusDto;
import com.cpu.process.Process;
import com.cpu.process.SchedulingRequest;
import com.cpu.processor.E_Processor;
import com.cpu.processor.P_Processor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final Map<String, CpuSystem> cpuSystemMap;

    public ResponseEntity<ResponseJson<Object>> run(SchedulingRequest request) {
        log.info(">>> 요청 알고리즘: {}", request.getAlgorithm());
        log.info(">>> 사용 가능한 알고리즘: {}", cpuSystemMap.keySet());
        CpuSystem system = cpuSystemMap.get(request.getAlgorithm().toUpperCase());
        log.info(">>> 선택된 CpuSystem: {}", system != null ? system.getClass().getSimpleName() : "null");
        if (system == null) {
            return ResponseEntity.notFound().build();
        }

        system.reset();
        if (request.getTimeQuantum() != null) {
            system.setTimeQuantum(request.getTimeQuantum());
        }

        // ▶ MCIQ 전용 vs 기존 알고리즘 프로세스 등록
        for (Process proc : request.getProcessList()) {
            if (system instanceof CpuSystem_MCIQ) {
                log.info("▶ MCIQ 프로세스 등록: {} [{}]", proc.getProcessName(), proc.getProcessTask());
                ((CpuSystem_MCIQ) system).addMciqProcess(
                        proc.getProcessName(),
                        proc.getArrivalTime(),
                        proc.getRemainTime(),
                        proc.getProcessTask()
                );
            } else {
                system.setProcess(
                        proc.getProcessName(),
                        proc.getArrivalTime(),
                        proc.getRemainTime()
                );
            }
        }

        // 코어 타입 세팅
        for (String type : request.getProcessorList()) {
            if ("P".equalsIgnoreCase(type)) {
                system.setProcessor(new P_Processor());
            } else if ("E".equalsIgnoreCase(type)) {
                system.setProcessor(new E_Processor());
            } else {
                return ResponseEntity.ok(ResponseJson.builder()
                        .name("schedule")
                        .message("Unknown processor type: " + type)
                        .build()
                );
            }
        }
        system.setProcessorCount(request.getProcessorList().size());

        // 시뮬레이션 실행
        int total = request.getProcessList().size();

        if (system instanceof CpuSystem_MCIQ mciq) {
            while (!mciq.isFinished()) {
                mciq.runOneClock();
                mciq.SetClockHistory();
                mciq.IncreaseProcessingTime();
            }
        } else {
            while (system.getTerminateProcessQueue().size() < total) {
                system.runOneClock();
                system.SetClockHistory();
                system.IncreaseProcessingTime();
            }
        }

        List<ProcessResultStatusDto> result = system.getProcessResultStatusList();
        return ResponseEntity.ok(ResponseJson.builder()
                .name("schedule success")
                .message("update ClockPoint")
                .data(system.getClockHistoryQueue())
                .result(result)
                .build()
        );
    }
}
