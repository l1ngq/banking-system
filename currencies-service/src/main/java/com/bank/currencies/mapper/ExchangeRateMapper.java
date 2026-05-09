package com.bank.currencies.mapper;

import com.bank.currencies.controller.dto.RateDto;
import com.bank.currencies.entity.ExchangeRateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExchangeRateMapper {

    RateDto toDto(ExchangeRateEntity entity);
}
