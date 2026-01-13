package com.nexus.dms.utils;

import java.util.Set;

public class CommonConstants {

    public static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png");

    public static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MB
    public static final int MAX_FILE_SIZE_MB = 20; // 20 MB

    public static final String PDF_VALUE = "application/pdf";
    public static final String JPEG_VALUE = "image/jpeg";
    public static final String JPG_VALUE = "image/jpg";
    public static final String PNG_VALUE = "image/png";

    public static final String AUTHORIZATION = "Authorization Bearer ";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String X_WWW_URL_ENCODED = "application/x-www-form-urlencoded";

    // Single bucket for all file types with folder structure
    public static final String MAIN_BUCKET = "nexus-scm";
    public static final String RETAILER_FOLDER = "nexus-retailer-dms-folder";
    public static final String SUPPLIER_FOLDER = "nexus-supplier-dms-folder";
    public static final String LOGISTICS_FOLDER = "nexus-logistics-dms-folder";
    public static final String COMMON_FOLDER = "nexus-common-dms-folder";

    public static final String RETAILER = "RETAILER";
    public static final String SUPPLIER = "SUPPLIER";
    public static final String LOGISTICS = "LOGISTICS";

}
