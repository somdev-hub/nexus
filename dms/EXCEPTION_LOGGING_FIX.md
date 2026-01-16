# Exception Logging Flow - Detailed Explanation

## Problem (Before Fix)
Exceptions were being logged **twice** with **different data**, creating duplicate entries in the database.

## Solution (After Fix)
Now only `ActivityLoggingAspect` logs exceptions, and `GlobalExceptionHandler` only handles transformation to HTTP responses.

---

## Flow Diagrams

### BEFORE (Double Logging - Problem)
```
Service throws ServiceLevelException
    ↓
ActivityLoggingAspect @Around
    ├─ Catches Exception
    ├─ Creates ActivityLogDto:
    │  - requestUrl: "/dms/upload/individual"
    │  - httpMethod: "POST"
    │  - responseStatus: 500
    │  - request: "{...DTO...}"  ✓ (captured from method args)
    │  - response: "{exceptionType, message}"  (exception details)
    │  - documentRecordId: null
    ├─ Logs to DB (FIRST TIME)
    └─ Re-throws exception
           ↓
GlobalExceptionHandler @ExceptionHandler
    ├─ Catches Exception
    ├─ Creates ErrorResponseDto:
    │  - title: "ServiceLevelException"
    │  - status: 500
    │  - message: "Error occurred while uploading individual file"
    ├─ Logs to DB (SECOND TIME)
    │  - requestUrl: "/dms/upload/individual"
    │  - httpMethod: "POST"
    │  - responseStatus: 500
    │  - request: null  ✗ (NOT captured in handler)
    │  - response: "{exceptionType, message}"
    │  - documentRecordId: null
    └─ Returns ErrorResponseDto to client
```

**Problem:** Two different log entries in database!

---

### AFTER (Single Logging - Solution)
```
Service throws ServiceLevelException
    ↓
ActivityLoggingAspect @Around
    ├─ Catches Exception
    ├─ Creates ActivityLogDto:
    │  - requestUrl: "/dms/upload/individual"
    │  - httpMethod: "POST"
    │  - responseStatus: 500
    │  - request: "{...DTO...}"  ✓ (captured from method args)
    │  - response: "{exceptionType, message}"
    │  - documentRecordId: null
    ├─ Logs to DB (ONLY ONCE) ✓
    └─ Re-throws exception
           ↓
GlobalExceptionHandler @ExceptionHandler
    ├─ Catches Exception
    ├─ Creates ErrorResponseDto
    ├─ NO LOGGING HERE ✓ (avoids duplicate)
    └─ Returns ErrorResponseDto to client
```

**Solution:** Single log entry with complete information!

---

## Database Entry Comparison

### Before Fix (Two Log Entries)

**Log Entry #1 (from ActivityLoggingAspect):**
```json
{
  "id": 101,
  "requestUrl": "/dms/upload/individual",
  "httpMethod": "POST",
  "responseStatus": 500,
  "request": "{\"userId\": 123, \"fileName\": \"document.pdf\", \"documentType\": \"PDF\"}",
  "response": "{\"exceptionType\": \"ServiceLevelException\", \"message\": \"Error occurred while uploading individual file\"}",
  "documentRecordId": null,
  "createdOn": "2024-01-15 10:30:45.123"
}
```

**Log Entry #2 (from GlobalExceptionHandler):**
```json
{
  "id": 102,
  "requestUrl": "/dms/upload/individual",
  "httpMethod": "POST",
  "responseStatus": 500,
  "request": null,
  "response": "{\"exceptionType\": \"ServiceLevelException\", \"message\": \"Error occurred while uploading individual file\"}",
  "documentRecordId": null,
  "createdOn": "2024-01-15 10:30:45.234"
}
```

❌ **Issues:**
- Two database entries for one request
- Different timestamps
- Request data missing in second log
- Wasted database space
- Harder to query/audit

---

### After Fix (Single Log Entry)

**Log Entry #1 (from ActivityLoggingAspect only):**
```json
{
  "id": 101,
  "requestUrl": "/dms/upload/individual",
  "httpMethod": "POST",
  "responseStatus": 500,
  "request": "{\"userId\": 123, \"fileName\": \"document.pdf\", \"documentType\": \"PDF\"}",
  "response": "{\"exceptionType\": \"ServiceLevelException\", \"message\": \"Error occurred while uploading individual file\"}",
  "documentRecordId": null,
  "createdOn": "2024-01-15 10:30:45.123"
}
```

✅ **Benefits:**
- Single database entry per request
- Complete request and response data
- Efficient database usage
- Easier to query and audit
- Clear cause-and-effect relationship

---

## Key Changes Made

### 1. GlobalExceptionHandler
**Removed:** All `logException()` calls and `Logger` dependency

```java
// BEFORE
@ExceptionHandler(ServiceLevelException.class)
public ResponseEntity<ErrorResponseDto> handleServiceLevelException(ServiceLevelException ex, HttpServletRequest request) {
    ErrorResponseDto errorResponse = new ErrorResponseDto(...);
    
    logException(request, HttpStatus.INTERNAL_SERVER_ERROR, ex);  // ❌ REMOVED
    
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
}

// AFTER
@ExceptionHandler(ServiceLevelException.class)
public ResponseEntity<ErrorResponseDto> handleServiceLevelException(ServiceLevelException ex, HttpServletRequest request) {
    ErrorResponseDto errorResponse = new ErrorResponseDto(...);
    
    // No logging here - let ActivityLoggingAspect handle it ✓
    
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
}
```

### 2. ActivityLoggingAspect
**Already handles both success and exceptions:**

```java
@Around("@annotation(logActivity)")
public Object logActivity(ProceedingJoinPoint joinPoint, LogActivity logActivity) throws Throwable {
    // ... setup ...
    
    try {
        result = joinPoint.proceed();  // Execute method
        // Handle success → log with response
        
    } catch (Exception e) {
        caughtException = e;           // Capture exception
        // Handle exception → log with exception details
    }
    
    // Log to DB (handles both success AND failure)
    logger.saveLogs(...);
    
    if (caughtException != null) {
        throw caughtException;  // Re-throw for GlobalExceptionHandler to transform
    }
    
    return result;
}
```

---

## Logging Responsibility Matrix

| Event | ActivityLoggingAspect | GlobalExceptionHandler |
|-------|:-------------------:|:--------------------:|
| Successful Request | ✓ Logs | ✗ No action |
| Service throws Exception | ✓ Logs + Re-throws | ✗ No logging (only transforms) |
| Request validation fails | ✓ Logs | ✗ No logging (only transforms) |

---

## Query Examples

### Find all failed requests for a specific endpoint

```sql
SELECT * FROM t_dms_logs 
WHERE request_url = '/dms/upload/individual' 
AND response_status >= 400
ORDER BY created_on DESC;
```

### Find all exceptions in the past hour

```sql
SELECT * FROM t_dms_logs 
WHERE response_status >= 400 
AND created_on >= NOW() - INTERVAL '1 hour'
ORDER BY created_on DESC;
```

### Find requests with missing request data

```sql
-- Should be ZERO after the fix
SELECT * FROM t_dms_logs 
WHERE request IS NULL 
AND response_status >= 400;
```

---

## Summary

| Aspect | Before | After |
|--------|--------|-------|
| Log entries per exception | 2 | 1 |
| Request data captured | Partial | Complete |
| Database efficiency | ❌ Poor | ✅ Good |
| Logging code in handlers | ✓ Yes | ✗ No |
| Separation of concerns | ❌ Poor | ✅ Good |
| Query complexity | Complex | Simple |
| Audit trail | Confusing | Clear |

---

## Testing the Fix

### Scenario 1: Successful Upload
1. Send valid upload request
2. Check logs - **1 entry** with status 200
3. Request and response both populated ✓

### Scenario 2: Authorization Fails
1. Send request without token
2. Service throws `UnauthorizedException`
3. Check logs - **1 entry** with status 401
4. Request (with token) and response both populated ✓

### Scenario 3: File Upload Error
1. Send large invalid file
2. Service throws `ServiceLevelException`
3. Check logs - **1 entry** with status 500
4. Request (with file info) and response (error details) both populated ✓

---

## Files Modified

1. **GlobalExceptionHandler.java**
   - ✓ Removed `Logger` autowiring
   - ✓ Removed `logException()` method
   - ✓ Removed all logging calls from exception handlers
   - ✓ Handlers now only transform exceptions to HTTP responses

2. **ActivityLoggingAspect.java**
   - ✓ Already logs both success and exceptions
   - ✓ Captures request body from method arguments
   - ✓ Captures response body and status
   - ✓ Re-throws exceptions for handler to process

---

This is the **production-ready** solution that follows **clean architecture** principles!

