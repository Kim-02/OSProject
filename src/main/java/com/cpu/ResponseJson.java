package com.cpu;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseJson<T> {
    String name;
    String message;
    T data;
    T result;
}
