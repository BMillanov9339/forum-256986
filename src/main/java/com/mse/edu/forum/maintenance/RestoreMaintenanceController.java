package com.mse.edu.forum.maintenance;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mse.edu.forum.service.ModerationAuditService;
import com.mse.edu.forum.service.TopicService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ops/restore")
@PreAuthorize("hasRole('ADMIN')")
public class RestoreMaintenanceController {

	private final RestoreMaintenanceState maintenanceState;
	private final ModerationAuditService audit;

	public RestoreMaintenanceController(RestoreMaintenanceState maintenanceState, ModerationAuditService audit) {
		this.maintenanceState = maintenanceState;
		this.audit = audit;
	}

	@GetMapping("/status")
	public RestoreMaintenanceStatusResponse status() {
		return new RestoreMaintenanceStatusResponse(
				maintenanceState.isRestoreInProgress(),
				maintenanceState.getRetryAfterSeconds(),
				OffsetDateTime.now(ZoneOffset.UTC));
	}

	@PostMapping("/enable")
	public RestoreMaintenanceStatusResponse enable(@RequestBody(required = false) RestoreMaintenanceToggleRequest request) {
		String reason = requireReason(request);
		maintenanceState.setRestoreInProgress(true);
		audit.record(TopicService.currentUser().getId(), "SYSTEM", 0, "RESTORE_ENABLE", "RESTORE", reason, false);
		return status();
	}

	@PostMapping("/disable")
	public RestoreMaintenanceStatusResponse disable(
			@RequestBody(required = false) RestoreMaintenanceToggleRequest request) {
		String reason = requireReason(request);
		maintenanceState.setRestoreInProgress(false);
		audit.record(TopicService.currentUser().getId(), "SYSTEM", 0, "RESTORE_DISABLE", "RESTORE", reason, false);
		return status();
	}

	private static String requireReason(RestoreMaintenanceToggleRequest request) {
		if (request == null || request.reason() == null || request.reason().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restore reason is required");
		}
		return request.reason().trim();
	}
}
