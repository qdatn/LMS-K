package com.example.hcm25_cpl_ks_java_01_lms.training_program_enrollment;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.classes.ClassesRepository;
import com.example.hcm25_cpl_ks_java_01_lms.classes.ClassesService;
import com.example.hcm25_cpl_ks_java_01_lms.course.Course;
import com.example.hcm25_cpl_ks_java_01_lms.course_enrollment.CourseEnrollmentService;
import com.example.hcm25_cpl_ks_java_01_lms.course.course_syllabus.CourseSyllabus;
import com.example.hcm25_cpl_ks_java_01_lms.course.course_syllabus.CourseSyllabusRepository;
import com.example.hcm25_cpl_ks_java_01_lms.training_program.TrainingProgram;
import com.example.hcm25_cpl_ks_java_01_lms.training_program.TrainingProgramRepository;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import com.example.hcm25_cpl_ks_java_01_lms.user_enrollment_training_program.UserEnrollmentTrainingProgramService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingProgramEnrollmentService {

    private final TrainingProgramEnrollmentRepository enrollmentRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ClassesRepository classesRepository;
    private final ClassesService classesService;
    private final UserEnrollmentTrainingProgramService userEnrollmentTrainingProgramService;
    private final CourseSyllabusRepository courseSyllabusRepository;
    private final CourseEnrollmentService courseEnrollmentService;

    @Autowired
    public TrainingProgramEnrollmentService(TrainingProgramEnrollmentRepository enrollmentRepository,
                                            TrainingProgramRepository trainingProgramRepository,
                                            ClassesRepository classesRepository,
                                            ClassesService classesService,
                                            UserEnrollmentTrainingProgramService userEnrollmentTrainingProgramService,
                                            CourseSyllabusRepository courseSyllabusRepository,
                                            CourseEnrollmentService courseEnrollmentService) {
        this.enrollmentRepository = enrollmentRepository;
        this.trainingProgramRepository = trainingProgramRepository;
        this.classesRepository = classesRepository;
        this.classesService = classesService;
        this.userEnrollmentTrainingProgramService = userEnrollmentTrainingProgramService;
        this.courseSyllabusRepository = courseSyllabusRepository;
        this.courseEnrollmentService = courseEnrollmentService;
    }

    public Page<TrainingProgramEnrollment> getAllEnrollments(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return enrollmentRepository.findAll(pageable);
    }

    public void enrollClasses(Long trainingProgramId, List<Long> classIds) {
        TrainingProgram trainingProgram = trainingProgramRepository.findById(trainingProgramId)
                .orElseThrow(() -> new RuntimeException("Training program not found"));

        List<Long> syllabusIds = trainingProgram.getSyllabuses()
                .stream()
                .map(s -> s.getId())
                .collect(Collectors.toList());

        List<Course> relatedCourses = courseSyllabusRepository.findBySyllabusIdIn(syllabusIds)
                .stream()
                .map(CourseSyllabus::getCourse)
                .distinct()
                .collect(Collectors.toList());

        for (Long classId : classIds) {
            Classes clazz = classesService.getClassById(classId);

            if (!isAlreadyEnrolled(trainingProgramId, classId)) {
                TrainingProgramEnrollment enrollment = TrainingProgramEnrollment.builder()
                        .enrolledClass(clazz)
                        .trainingProgram(trainingProgram)
                        .enrollmentDate(LocalDateTime.now())
                        .build();
                enrollmentRepository.save(enrollment);
            }

            List<User> students = clazz.getStudents().stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("STUDENT")))
                    .collect(Collectors.toList());

            List<Long> studentIds = students.stream().map(User::getId).collect(Collectors.toList());

            for (Course course : relatedCourses) {
                courseEnrollmentService.enrollStudents(course.getId(), studentIds);
            }

            userEnrollmentTrainingProgramService.enrollUsers(trainingProgramId, studentIds);
        }
    }

    private boolean isAlreadyEnrolled(Long trainingProgramId, Long classId) {
        return enrollmentRepository.findByTrainingProgramId(trainingProgramId)
                .stream()
                .anyMatch(enrollment -> enrollment.getEnrolledClass().getId().equals(classId));
    }

    public List<TrainingProgramEnrollment> getEnrollmentsByTrainingProgram(Long trainingProgramId) {
        return enrollmentRepository.findByTrainingProgramId(trainingProgramId);
    }

    public List<TrainingProgramEnrollment> getEnrollmentsByClass(Long classId) {
        return enrollmentRepository.findByEnrolledClassId(classId);
    }

    public List<TrainingProgramEnrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    @Transactional
    public void deleteEnrollmentById(Long enrollmentId) {
        TrainingProgramEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found with ID: " + enrollmentId));

        Classes clazz = enrollment.getEnrolledClass();
        TrainingProgram program = enrollment.getTrainingProgram();

        List<Long> syllabusIds = program.getSyllabuses().stream().map(s -> s.getId()).collect(Collectors.toList());
        List<Course> relatedCourses = courseSyllabusRepository.findBySyllabusIdIn(syllabusIds)
                .stream()
                .map(CourseSyllabus::getCourse)
                .distinct()
                .collect(Collectors.toList());

        List<Long> studentIds = clazz.getStudents().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("STUDENT")))
                .map(User::getId)
                .collect(Collectors.toList());

        for (Course course : relatedCourses) {
            for (Long studentId : studentIds) {
                courseEnrollmentService.getEnrollmentsByCourse(course.getId()).stream()
                        .filter(e -> e.getUser().getId().equals(studentId))
                        .forEach(e -> courseEnrollmentService.deleteCourseEnrollment(e.getId()));
            }
        }

        enrollmentRepository.deleteById(enrollmentId);
    }

    @Transactional
    public void deleteTrainingProgramsByIds(List<Long> ids) {
        for (Long id : ids) {
            deleteEnrollmentById(id);
        }
    }

    public List<Classes> getAvailableClassesForEnrollment(String keyword) {
        List<Classes> allClasses = classesRepository.findAll();
        List<Long> enrolledClassIds = enrollmentRepository.findAll()
                .stream()
                .map(enrollment -> enrollment.getEnrolledClass().getId())
                .collect(Collectors.toList());

        return allClasses.stream()
                .filter(c -> !enrolledClassIds.contains(c.getId()))
                .filter(c -> keyword == null || c.getName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

}