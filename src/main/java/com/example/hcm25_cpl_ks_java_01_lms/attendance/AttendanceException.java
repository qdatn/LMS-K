package com.example.hcm25_cpl_ks_java_01_lms.attendance;

public class AttendanceException extends RuntimeException {
    public static class ClassNotFoundException extends AttendanceException {
        public ClassNotFoundException(Long classId) {
            super("Class not found with ID: " + classId);
        }
    }

    public static class StudentNotFoundException extends AttendanceException {
        public StudentNotFoundException(Long studentId) {
            super("Student not found with ID: " + studentId);
        }
    }

    public static class LoadingDataException extends AttendanceException {
        public LoadingDataException(String message, Throwable cause) {
            super("Error loading data: " + message, cause);
        }
    }

    public static class CreationException extends AttendanceException {
        public CreationException(String message, Throwable cause) {
            super("Error creating attendance: " + message, cause);
        }
    }

    public static class DeletionException extends AttendanceException {
        public DeletionException(String message, Throwable cause) {
            super("Error deleting attendance: " + message, cause);
        }
    }

    public static class UpdateException extends AttendanceException {
        public UpdateException(String message, Throwable cause) {
            super("Error updating attendance: " + message, cause);
        }
    }

    public static class ExportException extends AttendanceException {
        public ExportException(String message, Throwable cause) {
            super("Error exporting attendance data: " + message, cause);
        }
    }

    public AttendanceException(String message) {
        super(message);
    }

    public AttendanceException(String message, Throwable cause) {
        super(message, cause);
    }
}