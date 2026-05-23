package com.dailyCode.paypal_user.transaction.mapper;

import com.dailyCode.paypal_user.transaction.dto.TransactionResponse;
import com.dailyCode.paypal_user.transaction.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "senderEmail",   source = "sender.email")
    @Mapping(target = "senderName",    expression = "java(t.getSender() != null ? t.getSender().getFirstName() + \" \" + t.getSender().getLastName() : null)")
    @Mapping(target = "receiverEmail", source = "receiver.email")
    @Mapping(target = "receiverName",  expression = "java(t.getReceiver() != null ? t.getReceiver().getFirstName() + \" \" + t.getReceiver().getLastName() : null)")
    TransactionResponse toResponse(Transaction t);
}
