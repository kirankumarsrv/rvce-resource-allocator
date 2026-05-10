package com.rvce.scas.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event published when an exam seating arrangement is published
 * Listeners can use this to trigger notifications, reports, etc.
 */
public class ExamPublishedEvent extends ApplicationEvent {
    private final UUID examId;
    private final String examName;
    private final UUID publishedBy;

    public ExamPublishedEvent(Object source, UUID examId, String examName, UUID publishedBy) {
        super(source);
        this.examId = examId;
        this.examName = examName;
        this.publishedBy = publishedBy;
    }

    public UUID getExamId() {
        return examId;
    }

    public String getExamName() {
        return examName;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }
}
