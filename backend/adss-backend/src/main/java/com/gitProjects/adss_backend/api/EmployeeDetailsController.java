package com.gitProjects.adss_backend.api;

import GlobalClasses.EmployeeToSend;
import ServiceLayer.HR.WrapperService;
import ServiceLayer.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeDetailsController {

    private final WrapperService wrapperService;

    public EmployeeDetailsController(WrapperService wrapperService) {
        this.wrapperService = wrapperService;
    }

    // HR-manager view of an employee
    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployeeById(@PathVariable int id,
                                             @RequestParam(defaultValue = "1") int managerId) {
        // use HRManagerService instead of EmployeeService
        Response res = wrapperService.hrManagerService.getEmployee(managerId, id);

        if (res.errorOccurred()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(res.getErrorMsg()));
        }

        EmployeeToSend employee = (EmployeeToSend) res.getReturnValue();
        return ResponseEntity.ok(employee);
    }
//    @GetMapping("/{id}/schedule")
//    public ResponseEntity<?> getEmployeeSchedule(@PathVariable int id,
//                                                 @RequestParam int date) {
//        // true schedule (actual shifts) from EmployeeService
//        Response res = wrapperService.employeeService.getWorkSchedule(id, date);
//
//        if (res.errorOccurred()) {
//            return ResponseEntity
//                    .status(HttpStatus.BAD_REQUEST)
//                    .body(new ErrorResponse(res.getErrorMsg()));
//        }
//
//        String[][][] schedule = (String[][][]) res.getReturnValue();
//
//        ScheduleResponse body = new ScheduleResponse();
//        body.employeeId = id;
//        body.date = date;
//        body.schedule = schedule;
//
//        return ResponseEntity.ok(body);
//    }
//
//    public static class ScheduleResponse {
//        public int employeeId;
//        public int date;
//        public String[][][] schedule;
//    }

    @PostMapping("/{id}/availability")
    public ResponseEntity<?> setAvailability(@PathVariable int id,
                                             @RequestBody AvailabilityRequest body) {
        // check if employee is allowed to change for this date
        Response canRes = wrapperService.employeeService.canChangeAvailability(body.date);
        if (canRes.errorOccurred() || !(Boolean) canRes.getReturnValue()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Cannot change availability for this date"));
        }

        boolean[][] availability = body.availability;

        Response res = wrapperService.employeeService.setAvailability(id, body.date, availability);

        if (res.errorOccurred()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(res.getErrorMsg()));
        }

        return ResponseEntity.ok(new MessageResponse(
                res.getReturnValue() != null ? res.getReturnValue().toString() : "Availability set"
        ));
    }

    public static class AvailabilityRequest {
        public int date;              // e.g. 20240602 (Sunday of the week)
        public boolean[][] availability; // [7][2] : [day][shift] true/false
    }

    public static class MessageResponse {
        public String message;
        public MessageResponse(String message) { this.message = message; }
    }


    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
