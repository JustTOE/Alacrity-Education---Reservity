package dev.tmmc.reservity.reservations.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link ReservationTag} ↔ display-string DB column ('Solo focus' etc.).
 * Auto-applied so entities just declare {@code private ReservationTag tag;}.
 */
@Converter(autoApply = true)
public class ReservationTagConverter implements AttributeConverter<ReservationTag, String> {

    @Override
    public String convertToDatabaseColumn(ReservationTag attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public ReservationTag convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ReservationTag.fromValue(dbData);
    }
}
