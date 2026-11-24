package com.xinke.common.core.domain.model;

import java.util.Set;

/** Account and explicit role changes, published before the surrounding commit. */
public record UserManagementChangedEvent(Set<Long> userIds) { }
