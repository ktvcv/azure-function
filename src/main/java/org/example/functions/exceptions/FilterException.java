package org.example.functions.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FilterException extends RuntimeException{

    private List<String> errorLogs = new ArrayList<>();

    public FilterException(final List<String> errorLogs) {
        super();
        this.errorLogs = errorLogs;
    }


}
