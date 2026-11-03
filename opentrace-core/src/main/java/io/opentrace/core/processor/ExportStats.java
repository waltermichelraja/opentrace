package io.opentrace.core.processor;

public record ExportStats(long exported, long dropped, long failed){}