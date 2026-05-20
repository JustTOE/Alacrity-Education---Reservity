package dev.tmmc.reservity.spaces.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link SpaceType} ↔ lowercase DB string. Auto-applied to every
 * {@code SpaceType} attribute via {@code autoApply = true}, so entities
 * just declare {@code private SpaceType type;} with no extra annotations.
 */
@Converter(autoApply = true)
public class SpaceTypeConverter implements AttributeConverter<SpaceType, String> {

    @Override
    public String convertToDatabaseColumn(SpaceType attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public SpaceType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SpaceType.fromValue(dbData);
    }
}
