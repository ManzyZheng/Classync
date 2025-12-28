package com.classync.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "page_locks", uniqueConstraints = @UniqueConstraint(columnNames = { "classroom_id", "page_number" }))
public class PageLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "is_locked")
    private Boolean isLocked;

    // 构造函数
    public PageLock() {
        this.isLocked = false;
    }

    @PrePersist
    public void prePersist() {
        if (isLocked == null) {
            isLocked = false;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(Long classroomId) {
        this.classroomId = classroomId;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Boolean getIsLocked() {
        return isLocked != null ? isLocked : false;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }

    @Override
    public String toString() {
        return "PageLock{" +
                "id=" + id +
                ", classroomId=" + classroomId +
                ", pageNumber=" + pageNumber +
                ", isLocked=" + isLocked +
                '}';
    }
}
