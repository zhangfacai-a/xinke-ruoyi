package com.xinke.datahub.service;

public record PreparedDataRow(int sourceRowNo, byte[] rowHash, Object[] values) { }
