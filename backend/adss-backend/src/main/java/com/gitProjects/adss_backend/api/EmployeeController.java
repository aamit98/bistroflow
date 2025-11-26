package com.gitProjects.adss_backend.api;

import GlobalClasses.EmployeeToSend;
import ServiceLayer.HR.WrapperService;
import ServiceLayer.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class EmployeeController {

    private final WrapperService wrapperService;

    public EmployeeController(WrapperService wrapperService) {
        this.wrapperService = wrapperService;
    }

    /**
     * Get all employees in a branch.
     *
     * For now we hard-code managerId = 1 (later we’ll plug in real auth).
     */
    @GetMapping("/{branchId}/employees")
    public ResponseEntity<?> getEmployeesInBranch(@PathVariable int branchId,
                                                  @RequestParam(defaultValue = "1") int managerId) {
        Response res = wrapperService.hrManagerService.getAllEmployeesInBranch(managerId, branchId);

        if (res.errorOccurred()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(res.getErrorMsg()));
        }

        EmployeeToSend[] employees = (EmployeeToSend[]) res.getReturnValue();
        return ResponseEntity.ok(employees);
    }


    @GetMapping("/{branchId}/availability")
    public ResponseEntity<?> getBranchAvailability(@PathVariable int branchId,
                                                   @RequestParam int date,
                                                   @RequestParam(defaultValue = "1") int managerId) {
        Response res = wrapperService.hrManagerService.getEmployeesAvailability(managerId, date, branchId);

        if (res.errorOccurred()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(res.getErrorMsg()));
        }

        @SuppressWarnings("unchecked")
        List<String>[][] matrix = (List<String>[][]) res.getReturnValue();

        BranchAvailabilityResponse body = BranchAvailabilityResponse.fromMatrix(branchId, date, matrix);
        return ResponseEntity.ok(body);
    }

    static class BranchAvailabilityResponse {
        public int branchId;
        public int date;
        public List<List<List<String>>> availability; // [day][shift][employees]

        public static BranchAvailabilityResponse fromMatrix(int branchId, int date, List<String>[][] matrix) {
            BranchAvailabilityResponse resp = new BranchAvailabilityResponse();
            resp.branchId = branchId;
            resp.date = date;
            resp.availability = new ArrayList<>();

            for (int i = 0; i < matrix.length; i++) {
                List<List<String>> day = new ArrayList<>();
                for (int j = 0; j < matrix[i].length; j++) {
                    day.add(matrix[i][j] != null ? matrix[i][j] : List.of());
                }
                resp.availability.add(day);
            }
            return resp;
        }
    }
    public ResponseEntity<?> postSchedule(@PathVariable int branchId,
                                          @RequestParam(defaultValue = "1") int managerId,
                                          @RequestBody ScheduleRequest body) {

        int[][][] employeesIds = body.employeesIds;

        Response res = wrapperService.hrManagerService.postWorkSchedule(
                managerId,
                branchId,
                body.date,
                employeesIds
        );

        if (res.errorOccurred()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(res.getErrorMsg()));
        }

        return ResponseEntity.ok(new MessageResponse("Schedule posted successfully"));
    }
    public static class MessageResponse {
        public String message;
        public MessageResponse(String message) { this.message = message; }
    }

    public static class ScheduleRequest {
        public int date;          // e.g. 20240714 - week key
        public int[][][] employeesIds; // [7][2][k] employees IDs in each slot
    }
    // Simple error DTO so the JSON is nice
    static class ErrorResponse {
        public final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
