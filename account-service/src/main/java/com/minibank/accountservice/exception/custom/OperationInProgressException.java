package com.minibank.accountservice.exception.custom;

public class OperationInProgressException  extends RuntimeException{
    public OperationInProgressException (String message){
        super(message);
    }
}
