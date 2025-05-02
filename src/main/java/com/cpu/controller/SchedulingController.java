package com.cpu.controller;


import com.cpu.ResponseJson;
import com.cpu.process.SchedulingRequest;
import com.cpu.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;

    @PostMapping("/run")
    public ResponseEntity<ResponseJson<Object>> runSchedule(@RequestBody SchedulingRequest schedulingRequest) {
        return schedulingService.run(schedulingRequest);
    }
}
