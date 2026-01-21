package com.nexus.hr.config;

import com.nexus.hr.utils.EncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter for automatic encryption and decryption of String fields
 */
@Converter
@Component
public class StringEncryptionConverter implements AttributeConverter<String, String> {

    private static EncryptionUtil encryptionUtilInstance;

    @Autowired
    public StringEncryptionConverter(EncryptionUtil encryptionUtil) {
        StringEncryptionConverter.encryptionUtilInstance = encryptionUtil;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionUtilInstance.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptionUtilInstance.decrypt(dbData);
    }
}
