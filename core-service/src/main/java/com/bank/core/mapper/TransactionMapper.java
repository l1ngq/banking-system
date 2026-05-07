package com.bank.core.mapper;

import com.bank.core.dto.TransactionDto;
import com.bank.core.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {

    TransactionDto toDto(TransactionEntity entity);
}
