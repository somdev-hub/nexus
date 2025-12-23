package com.nexus.core.utils;

import org.springframework.beans.factory.annotation.Autowired;

import com.nexus.core.entities.Logs;
import com.nexus.core.repository.LogsRepo;

public class Logger {

    @Autowired
    private LogsRepo logsRepo;

    public void log(Logs log) {
        try {
            logsRepo.save(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
