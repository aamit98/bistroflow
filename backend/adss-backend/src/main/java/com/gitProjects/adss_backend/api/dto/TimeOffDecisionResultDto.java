package com.gitProjects.adss_backend.api.dto;

public record TimeOffDecisionResultDto(
        TimeOffRequestDto request,
        boolean shiftRemoved,
        boolean scheduleWasPublished
) {}
