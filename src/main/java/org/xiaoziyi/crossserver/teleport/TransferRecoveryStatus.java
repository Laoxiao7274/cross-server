package org.xiaoziyi.crossserver.teleport;

public enum TransferRecoveryStatus {
	NONE,
	PREPARING,
	IN_FLIGHT,
	ARRIVED_AWAITING_ACK,
	EXPIRED_NEEDS_CLEANUP,
	FAILED_NEEDS_CLEANUP,
	ADMIN_CLEARED,
	COMPLETED,
	RECOVERED,
	UNKNOWN
}
