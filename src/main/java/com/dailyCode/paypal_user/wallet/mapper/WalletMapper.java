package com.dailyCode.paypal_user.wallet.mapper;

import com.dailyCode.paypal_user.wallet.dto.WalletResponse;
import com.dailyCode.paypal_user.wallet.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "ownerName", expression = "java(wallet.getUser().getFirstName() + \" \" + wallet.getUser().getLastName())")
    WalletResponse toResponse(Wallet wallet);
}
