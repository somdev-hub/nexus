package com.nexus.dms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploaderResponse {

    private String fileName;

    private String dmsId;

    private String url;

    private String directoryLocation;
}
