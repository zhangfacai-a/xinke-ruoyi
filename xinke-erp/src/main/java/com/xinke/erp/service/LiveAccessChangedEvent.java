package com.xinke.erp.service;

import java.util.Set;

public record LiveAccessChangedEvent(Set<Long> userIds) {}
