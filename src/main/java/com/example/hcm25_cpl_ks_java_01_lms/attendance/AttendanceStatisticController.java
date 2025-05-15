package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/attendance")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Attendance')")
public class AttendanceStatisticController {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceStatisticController.class);

    private final AttendanceService attendanceService;
    private final AttendanceStatisticService statisticService;

    @Autowired
    public AttendanceStatisticController(
            AttendanceService attendanceService,
            AttendanceStatisticService statisticService) {
        this.attendanceService = attendanceService;
        this.statisticService = statisticService;
    }

    @GetMapping
    public String listAttendanceDates(@RequestParam(required = false) String searchTerm, Model model) {
        try {
            List<Classes> originalClasses = attendanceService.getActiveClassesForAttendance();
            List<AttendanceDTO> activeClasses = convertClassesToAttendanceDTOs(originalClasses);
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String searchTermLower = searchTerm.toLowerCase();
                activeClasses = activeClasses.stream()
                        .filter(c -> (c.getName() != null && c.getName().toLowerCase().contains(searchTermLower)) ||
                                (c.getClassCode() != null && c.getClassCode().toLowerCase().contains(searchTermLower)))
                        .collect(Collectors.toList());
            }
            int totalClasses = activeClasses.size();
            int totalStudents = activeClasses.stream().mapToInt(AttendanceDTO::getStudentCount).sum();
            int totalAttendanceDates = statisticService.getTotalAttendanceDates();
            double averageAttendanceRate = statisticService.getAverageAttendanceRate();

            Map<Long, LocalDate> classLastAttendance = new HashMap<>();
            for (AttendanceDTO clazz : activeClasses) {
                List<LocalDate> dates = attendanceService.getAttendanceDatesForClass(clazz.getId());
                if (!dates.isEmpty()) {
                    classLastAttendance.put(clazz.getId(), dates.get(0));
                }
            }

            model.addAttribute("activeClasses", activeClasses);
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("searchTerm", searchTerm);
            model.addAttribute("totalClasses", totalClasses);
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("totalAttendanceDates", totalAttendanceDates);
            model.addAttribute("averageAttendanceRate", averageAttendanceRate);
            model.addAttribute("classLastAttendance", classLastAttendance);
            model.addAttribute("classAttendanceStats", statisticService.getClassAttendanceStats());
            model.addAttribute("attendanceStatusStats", statisticService.getAttendanceStatusStats());
            model.addAttribute("content", "attendance/classes");

            return Constants.LAYOUT;
        } catch (Exception e) {
            logger.error("Error loading class list: {}", e.getMessage(), e);
            throw new AttendanceException.LoadingDataException("Failed to load class list", e);
        }
    }

    @GetMapping("/charts")
    public String showAttendanceCharts(Model model) {
        try {
            List<Classes> originalClasses = attendanceService.getActiveClassesForAttendance();
            List<AttendanceDTO> activeClasses = convertClassesToAttendanceDTOs(originalClasses);

            Map<Long, AttendanceStatsDTO> classAttendanceStats = statisticService.getClassAttendanceStats();
            double averageAttendanceRate = statisticService.getAverageAttendanceRate();
            Map<String, Double> attendanceStatusStats = statisticService.getAttendanceStatusStats();

            logger.info("Chart data: activeClasses={}, statsClasses={}",
                    (activeClasses != null ? activeClasses.size() : 0),
                    (classAttendanceStats != null ? classAttendanceStats.size() : 0));

            int totalStudents = activeClasses.stream().mapToInt(AttendanceDTO::getStudentCount).sum();
            int totalAttendanceDates = statisticService.getTotalAttendanceDates();

            model.addAttribute("activeClasses", activeClasses);
            model.addAttribute("classAttendanceStats", classAttendanceStats);
            model.addAttribute("averageAttendanceRate", averageAttendanceRate);
            model.addAttribute("attendanceStatusStats", attendanceStatusStats);
            model.addAttribute("totalClasses", activeClasses.size());
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("totalAttendanceDates", totalAttendanceDates);
            model.addAttribute("content", "attendance/charts");

            return Constants.LAYOUT;
        } catch (Exception e) {
            logger.error("Error loading chart data: {}", e.getMessage(), e);
            throw new AttendanceException.LoadingDataException("Failed to load chart data", e);
        }
    }

    private List<AttendanceDTO> convertClassesToAttendanceDTOs(List<Classes> classes) {
        return classes.stream()
                .map(cls -> new AttendanceDTO(
                        cls.getId(),
                        cls.getName(),
                        cls.getClassCode(),
                        cls.getStartDate(),
                        cls.getEndDate(),
                        cls.getIsActive(),
                        cls.getStudents() != null ? cls.getStudents().size() : 0
                ))
                .collect(Collectors.toList());
    }
}