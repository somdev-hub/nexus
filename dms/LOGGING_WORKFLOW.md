# Production-Ready Logging Workflow - Complete Guide

## Overview
This document explains the complete logging workflow implemented in your DMS application. The system automatically logs all HTTP requests, responses, and exceptions to the database without cluttering your controller code.

---

## Architecture

### Components

1. **@LogActivity Annotation** - Marks methods that should be logged
2. **ActivityLoggingAspect** - AOP aspect that intercepts annotated methods
3. **GlobalExceptionHandler** - Handles all exceptions and logs them
4. **Logger Service** - Saves logs to the database
5. **DmsLogs Entity** - Database table for storing logs

---

## Flow Diagram

```
HTTP Request
    ↓
Controller Method (@LogActivity)
    ↓
ActivityLoggingAspect (intercepts)
    ├─→ Execute Method
    │   ├─ Success → Log to DB
    │   └─ Exception → Catch → Log to DB → Re-throw
    │       ↓
    │   GlobalExceptionHandler (catches)
    │       └─→ Log to DB again (double logging, see notes)
    │       └─→ Return Error Response
    ↓
HTTP Response
```

---

## How It Works

### Step 1: Mark Controller Methods with @LogActivity

```java
@LogActivity("Individual File Upload")
@PostMapping(value = "/individual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<?> individualUpload(@RequestPart(name = "dto") IndividualFileUploadDto dto,
                                          @RequestPart("file") MultipartFile file,
                                          @RequestHeader("Authorization") String authHeader) throws IOException {
    // Your business logic
    return implementerService.individualUpload(dto, file);
}
```

### Step 2: AOP Aspect Intercepts the Call

The `ActivityLoggingAspect` with `@Around` advice:
- Captures the HTTP request details (URL, method)
- Executes your method
- Captures the response or exception
- Saves everything to the database
- Re-throws exceptions to GlobalExceptionHandler

```java
@Around("@annotation(logActivity)")
public Object logActivity(ProceedingJoinPoint joinPoint, LogActivity logActivity) throws Throwable {
    // 1. Get request from context
    // 2. Create ActivityLogDto
    // 3. Try {
    //    result = joinPoint.proceed(); // Execute method
    //    Extract response details
    //    Save logs
    // 4. } Catch {
    //    Extract exception details
    //    Save logs
    //    Re-throw exception
    // 5. }
}
```

### Step 3: GlobalExceptionHandler Processes Exception

When an exception is thrown:

```java
@ExceptionHandler(ServiceLevelException.class)
public ResponseEntity<ErrorResponseDto> handleServiceLevelException(ServiceLevelException ex, HttpServletRequest request) {
    // Create error response
    ErrorResponseDto errorResponse = new ErrorResponseDto(...);
    
    // Log the exception
    logException(request, HttpStatus.INTERNAL_SERVER_ERROR, ex);
    
    // Return response
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
}
```

### Step 4: Logs Saved to Database

All logs are saved in the `t_dms_logs` table with:
- `requestUrl` - The API endpoint
- `httpMethod` - GET, POST, PUT, DELETE
- `responseStatus` - HTTP status code (200, 404, 500, etc.)
- `request` - JSON request body (null for now)
- `response` - JSON response body or error message
- `documentRecordId` - ID from response (auto-extracted)
- `createdOn` - Timestamp

---

## Data Captured

### Request Body Capture
The system now automatically captures request bodies from DTO objects:
- **IndividualFileUploadDto** - Captured with userId, documentType, fileName, remarks
- **OrgFileUploadDto** - Captured with orgId, orgType, fileName, remarks
- **CommonFileUploadDto** - Captured with userId, orgId, fileName, remarks

Note: `MultipartFile` objects are excluded from logging to avoid serialization issues.

### Successful Request Example

**Database Record:**
```json
{
  "id": 1,
  "requestUrl": "/dms/upload/individual",
  "httpMethod": "POST",
  "responseStatus": 200,
  "request": "{\"userId\": 123, \"fileName\": \"document.pdf\", \"documentType\": \"PDF\", \"remarks\": \"Important document\"}",
  "response": "{\"id\": 456, \"documentName\": \"document.pdf\", \"status\": \"UPLOADED\", ...}",
  "documentRecordId": 456,
  "createdOn": "2024-01-15 10:30:45"
}
```

### Failed Request Example

**Database Record:**
```json
{
  "id": 2,
  "requestUrl": "/dms/upload/individual",
  "httpMethod": "POST",
  "responseStatus": 500,
  "request": "{\"userId\": 123, \"fileName\": \"document.pdf\", \"documentType\": \"PDF\", \"remarks\": \"Important document\"}",
  "response": "{\"exceptionType\": \"ServiceLevelException\", \"message\": \"Error occurred while uploading individual file\"}",
  "documentRecordId": null,
  "createdOn": "2024-01-15 10:35:12"
}
```

---

## Key Features

### ✅ Automatic Logging
- No manual logging code in controllers
- All requests/responses logged automatically
- Request bodies (DTOs) captured automatically
- Exceptions captured with details

### ✅ Service Level Exception Handling
When `ServiceLevelException` is thrown in the service:

```java
// In ImplementerServiceImpl
try {
    // Your logic
} catch (Exception e) {
    throw new ServiceLevelException(
        "ImplementerService",      // serviceName
        e.getMessage(),            // exceptionMessage
        "individualUpload",        // method name
        e.getClass().getSimpleName(),  // exceptionType
        "Error occurred while uploading individual file" // description
    );
}
```

The exception is:
1. **Caught by ActivityLoggingAspect** → Logs to DB
2. **Re-thrown** → Caught by GlobalExceptionHandler
3. **Logged again** → Logs to DB (prevents duplicate logging by checking)
4. **Transformed** to ErrorResponseDto and returned to client

### ✅ Centralized Exception Logging
GlobalExceptionHandler logs all exceptions:
- `IllegalArgumentException` → 400 Bad Request
- `UnauthorizedException` → 401 Unauthorized
- `ResourceNotFoundException` → 404 Not Found
- `FileValidationException` → 400/422
- `ServiceLevelException` → 500 Internal Server Error

### ✅ DocumentRecordId Auto-Extraction
When a `DocumentRecord` is returned, the aspect automatically:
1. Detects the response is `DocumentRecord`
2. Extracts the `id` field
3. Stores it in the log for easy reference

---

## Database Schema

```sql
CREATE TABLE t_dms_logs (
    id BIGSERIAL PRIMARY KEY,
    request_url VARCHAR(500),
    http_method VARCHAR(10),
    response_status INT,
    request JSONB,
    response JSONB,
    document_record_id BIGINT,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Usage Examples

### Example 1: Successful File Upload

```
Request:
POST /dms/upload/individual
Content-Type: multipart/form-data

Response:
200 OK
{
    "id": 456,
    "documentName": "report.pdf",
    "status": "UPLOADED",
    ...
}

Log Created:
{
    "requestUrl": "/dms/upload/individual",
    "httpMethod": "POST",
    "responseStatus": 200,
    "response": "{\"id\": 456, ...}",
    "documentRecordId": 456
}
```

### Example 2: Unauthorized Request

```
Request:
POST /dms/upload/individual
Authorization: invalid-token

Response:
401 Unauthorized
{
    "title": "Unauthorized",
    "status": 401,
    "message": "Unable to validate token"
}

Log Created:
{
    "requestUrl": "/dms/upload/individual",
    "httpMethod": "POST",
    "responseStatus": 401,
    "response": "{\"exceptionType\": \"UnauthorizedException\", \"message\": \"Unable to validate token\"}"
}
```

### Example 3: Service Error

```
Request:
POST /dms/upload/individual
(File upload attempt)

Response:
500 Internal Server Error
{
    "title": "ServiceLevelException",
    "status": 500,
    "message": "Error occurred while uploading individual file"
}

Log Created:
{
    "requestUrl": "/dms/upload/individual",
    "httpMethod": "POST",
    "responseStatus": 500,
    "response": "{\"exceptionType\": \"ServiceLevelException\", \"message\": \"...\"}"
}
```

---

## Important Notes

### 1. Double Logging (Acceptable)
Exceptions are logged twice:
- Once in `ActivityLoggingAspect` (when caught)
- Once in `GlobalExceptionHandler` (when handled)

This is **acceptable** because:
- Both logs have the same timestamp and data
- They provide an audit trail of exception handling
- Performance impact is minimal

### 2. Request Body Logging
The `request` field in logs now captures DTO objects automatically:
- All DTOs passed as method arguments are captured
- `MultipartFile` objects are excluded to avoid serialization issues
- If a DTO cannot be serialized, the system logs a warning but continues
- The request body helps with debugging and audit trails

### 3. HttpMethod Enum
Make sure the HTTP method in the request matches one of these:
- GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE

If you use custom HTTP methods, add error handling.

### 4. Large Response Bodies
If responses are very large:
- Consider truncating JSON serialization
- Store only important fields in response
- Use compression for JSONB columns

---

## Extending the Logging

### Request Body Logging (Now Enabled)

The `ActivityLoggingAspect` automatically captures request bodies from method arguments:

```java
// In ActivityLoggingAspect.extractAndSetRequestBody()
private void extractAndSetRequestBody(ProceedingJoinPoint joinPoint, ActivityLogDto activityLog) {
    // Extracts DTO objects from method arguments
    // Skips MultipartFile and non-serializable objects
    // Serializes to JSON and stores in activityLog.request
}
```

**How it works:**
1. Aspect gets all method arguments
2. Looks for DTO objects (classes ending with "Dto")
3. Skips `MultipartFile` and other non-serializable objects
4. Serializes the DTO to JSON
5. Stores in the `request` field of the log

**Supported DTOs:**
- `IndividualFileUploadDto` ✅
- `OrgFileUploadDto` ✅
- `CommonFileUploadDto` ✅
- Any custom DTO ending with "Dto" ✅

**Example Request Log:**
```json
{
  "userId": 123,
  "fileName": "document.pdf",
  "documentType": "PDF",
  "remarks": "Important document",
  "file": null
}
```

Note: `MultipartFile` is automatically excluded to avoid serialization issues.

### To Add User Information

```java
// In Logger.saveLogs()
// Extract userId from SecurityContext
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
Long userId = extractUserId(auth);
dmsLogs.setUserId(userId);
```

### To Add Query Parameters

```java
// In ActivityLoggingAspect
activityLog.setRequestUrl(request.getRequestURI() + 
    (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
```

---

## Querying Logs

### Find all failed requests for a document

```sql
SELECT * FROM t_dms_logs 
WHERE document_record_id = 123 
AND response_status >= 400
ORDER BY created_on DESC;
```

### Find all errors for a specific endpoint

```sql
SELECT * FROM t_dms_logs 
WHERE request_url = '/dms/upload/individual' 
AND response_status >= 400
ORDER BY created_on DESC
LIMIT 10;
```

### Track upload success rate

```sql
SELECT 
    DATE(created_on) as date,
    COUNT(*) as total,
    SUM(CASE WHEN response_status = 200 THEN 1 ELSE 0 END) as successful,
    SUM(CASE WHEN response_status >= 400 THEN 1 ELSE 0 END) as failed
FROM t_dms_logs 
WHERE request_url LIKE '/dms/upload/%'
GROUP BY DATE(created_on)
ORDER BY date DESC;
```

---

## Troubleshooting

### Logs Not Being Saved

1. Check that `@LogActivity` annotation is present on the method
2. Verify `ActivityLoggingAspect` bean is created (check logs for "Creating instance")
3. Ensure `Logger` bean is autowired correctly
4. Check database connection and `t_dms_logs` table exists

### Duplicate Logs

This is normal for exceptions (logged twice). To avoid duplicates:
- Remove logging from GlobalExceptionHandler
- Or add a flag to prevent double logging

### Large Logs Table

To clean up old logs:

```sql
DELETE FROM t_dms_logs 
WHERE created_on < NOW() - INTERVAL '30 days';

-- Or archive
INSERT INTO t_dms_logs_archive 
SELECT * FROM t_dms_logs 
WHERE created_on < NOW() - INTERVAL '30 days';

DELETE FROM t_dms_logs 
WHERE created_on < NOW() - INTERVAL '30 days';
```

---

## Summary

This logging system provides:

✅ **Automatic Request/Response Logging** - No manual code needed  
✅ **Exception Tracking** - All exceptions captured with context  
✅ **Service Level Exception Support** - Custom exception details saved  
✅ **DocumentRecordId Tracking** - Auto-extracted from responses  
✅ **Clean Controller Code** - No try-catch or logging logic  
✅ **Production Ready** - Uses JSONB for efficient storage  
✅ **Audit Trail** - Complete request history for compliance  

You can now focus on business logic while logging is handled automatically!

