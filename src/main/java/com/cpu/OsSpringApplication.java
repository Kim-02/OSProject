package com.cpu;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import java.awt.*;
import java.net.URI;

@SpringBootApplication
public class OsSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(OsSpringApplication.class, args);
    }
    @EventListener(ApplicationReadyEvent.class)
    public void openBrowserFallback() {
        String url = "http://localhost:8080";
        try {
            // Windows
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec(new String[] {
                        "rundll32", "url.dll,FileProtocolHandler", url
                });
            }
            // macOS
            else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                Runtime.getRuntime().exec("open " + url);
            }
            // Linux (xdg-open 지원)
            else {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
