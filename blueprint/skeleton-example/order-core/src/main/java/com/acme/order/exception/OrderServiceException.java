package com.acme.order.exception;

import com.acme.platform.core.ServiceException;
import java.util.List;

/**
 * order servisinin tipli istisnasi (referans Bolum 7.1). details istemciye DONER (yalniz gosterilebilir k=v),
 * safeLogReason/safeLogCategory YALNIZ log icindir (Bolum 7.4): ikisi asla karistirilmaz.
 */
public class OrderServiceException extends ServiceException {
    private final List<String> details;
    private final String safeLogReason;
    private final String safeLogCategory;

    public OrderServiceException(ErrorCode code) { this(code, null, null, List.of()); }

    public OrderServiceException(ErrorCode code, String safeLogReason) { this(code, safeLogReason, null, List.of()); }

    public OrderServiceException(ErrorCode code, String safeLogReason, String safeLogCategory, List<String> details) {
        super(code);
        this.safeLogReason = safeLogReason;
        this.safeLogCategory = safeLogCategory;
        this.details = List.copyOf(details);
    }

    public List<String> getDetails() { return details; }
    public String getSafeLogReason() { return safeLogReason; }
    public String getSafeLogCategory() { return safeLogCategory; }
}
