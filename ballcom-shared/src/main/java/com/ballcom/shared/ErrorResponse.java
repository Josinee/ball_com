package com.ballcom.shared;

public record ErrorResponse (
    int status,
    String message
){
    
}
