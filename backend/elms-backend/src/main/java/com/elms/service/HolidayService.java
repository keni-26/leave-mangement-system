package com.elms.service;

import com.elms.entity.Holiday;
import com.elms.repository.HolidayRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAll().stream()
                .sorted((left, right) -> left.getHolidayDate().compareTo(right.getHolidayDate()))
                .toList();
    }

    public Holiday getHolidayById(Long id) {
        return holidayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Holiday not found"));
    }

    public Holiday createHoliday(Holiday holiday) {
        validateDuplicateHolidayDate(holiday.getHolidayDate(), null);

        LocalDateTime now = LocalDateTime.now();
        holiday.setCreatedAt(now);
        holiday.setUpdatedAt(now);

        try {
            return holidayRepository.save(holiday);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Holiday date already exists");
        }
    }

    public Holiday updateHoliday(Long id, Holiday updatedHoliday) {
        Holiday existingHoliday = holidayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Holiday not found"));

        validateDuplicateHolidayDate(updatedHoliday.getHolidayDate(), id);

        existingHoliday.setName(updatedHoliday.getName());
        existingHoliday.setHolidayDate(updatedHoliday.getHolidayDate());
        existingHoliday.setDescription(updatedHoliday.getDescription());

        return holidayRepository.save(existingHoliday);
    }

    public void deleteHoliday(Long id) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Holiday not found"));
        holidayRepository.delete(holiday);
    }

    private void validateDuplicateHolidayDate(java.time.LocalDate holidayDate, Long excludeId) {
        if (holidayDate == null) {
            return;
        }

        Optional<Holiday> existingHoliday = holidayRepository.findAll().stream()
                .filter(holiday -> holiday.getHolidayDate() != null && holiday.getHolidayDate().equals(holidayDate))
                .filter(holiday -> excludeId == null || !excludeId.equals(holiday.getId()))
                .findFirst();

        if (existingHoliday.isPresent()) {
            throw new IllegalStateException("Holiday date already exists");
        }
    }
}
