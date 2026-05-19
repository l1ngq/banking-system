package com.bank.core.mapper;

import com.bank.core.dto.AccountDto;
import com.bank.core.entity.BankAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountMapper {

    AccountDto toDto(BankAccountEntity entity);

    List<AccountDto> toDtoList(List<BankAccountEntity> entities);
}
