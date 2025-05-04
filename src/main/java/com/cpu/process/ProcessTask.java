package com.cpu.process;

public enum ProcessTask {
    ATTITUDE_ESTIMATION("자세 추정"),
    ATTITUDE_CONTROL("자세 제어"),
    RATE_CONTROL("속도 제어"),
    MOTOR_MIXER("모터 믹서"),
    GPS_PARSER("GPS 파서"),
    BAROMETER_HANDLE("기압계 처리"),
    MAGNETOMETER_HANDLE("자력계 처리"),
    RC_RECEIVER("RC 수신기"),
    AUTO_LANDING("자동 착륙"),
    FAILSAFE_HANDLER("Failsafe 처리"),
    BATTERY_MONITOR("배터리 모니터링"),
    TEMPERATURE_MONITOR("온도 모니터링");

    private final String label;

    ProcessTask(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ProcessTask fromLabel(String label) {
        for (ProcessTask task : values()) {
            if (task.label.equals(label)) {
                return task;
            }
        }
        throw new IllegalArgumentException("No ProcessTask with label: " + label);
    }
}
