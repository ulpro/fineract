/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.paymentdetail.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.paymentdetail.PaymentDetailConstants;
import org.apache.fineract.portfolio.paymentdetail.data.PaymentDetailData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentType;

@Entity
@Getter
@Table(name = "m_payment_detail")
public class PaymentDetail extends AbstractPersistableCustom<Long> {

    @ManyToOne
    @JoinColumn(name = "payment_type_id", nullable = false)
    private PaymentType paymentType;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "check_number", length = 50)
    private String checkNumber;

    @Column(name = "routing_code", length = 50)
    private String routingCode;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(name = "bank_number", length = 50)
    private String bankNumber;

    // Depositor information fields
    @Column(name = "depositor_name", length = 150)
    private String depositorName;

    @Column(name = "depositor_nationality", length = 100)
    private String depositorNationality;

    @Column(name = "depositor_id_type", length = 50)
    private String depositorIdType;

    @Column(name = "depositor_id_number", length = 100)
    private String depositorIdNumber;

    @Column(name = "depositor_id_issued_at", length = 150)
    private String depositorIdIssuedAt;

    @Column(name = "depositor_purpose", length = 255)
    private String depositorPurpose;

    protected PaymentDetail() {}

    public static PaymentDetail generatePaymentDetail(final PaymentType paymentType, final JsonCommand command,
            final Map<String, Object> changes) {
        final String accountNumber = command.stringValueOfParameterNamed(PaymentDetailConstants.accountNumberParamName);
        final String checkNumber = command.stringValueOfParameterNamed(PaymentDetailConstants.checkNumberParamName);
        final String routingCode = command.stringValueOfParameterNamed(PaymentDetailConstants.routingCodeParamName);
        final String receiptNumber = command.stringValueOfParameterNamed(PaymentDetailConstants.receiptNumberParamName);
        final String bankNumber = command.stringValueOfParameterNamed(PaymentDetailConstants.bankNumberParamName);
        final String depositorName = command.stringValueOfParameterNamed(PaymentDetailConstants.depositorNameParamName);
        final String depositorNationality = command.stringValueOfParameterNamed(PaymentDetailConstants.depositorNationalityParamName);
        final String depositorIdType = command.stringValueOfParameterNamed(PaymentDetailConstants.depositorIdTypeParamName);
        final String depositorIdNumber = command.stringValueOfParameterNamed(PaymentDetailConstants.depositorIdNumberParamName);
        final String depositorIdIssuedAt = command.stringValueOfParameterNamed(PaymentDetailConstants.depositorIdIssuedAtParamName);
        final String depositorPurpose = command.stringValueOfParameterNamed(PaymentDetailConstants.depositorPurposeParamName);

        if (StringUtils.isNotBlank(accountNumber)) {
            changes.put(PaymentDetailConstants.accountNumberParamName, accountNumber);
        }
        if (StringUtils.isNotBlank(checkNumber)) {
            changes.put(PaymentDetailConstants.checkNumberParamName, checkNumber);
        }
        if (StringUtils.isNotBlank(routingCode)) {
            changes.put(PaymentDetailConstants.routingCodeParamName, routingCode);
        }
        if (StringUtils.isNotBlank(receiptNumber)) {
            changes.put(PaymentDetailConstants.receiptNumberParamName, receiptNumber);
        }
        if (StringUtils.isNotBlank(bankNumber)) {
            changes.put(PaymentDetailConstants.bankNumberParamName, bankNumber);
        }
        if (StringUtils.isNotBlank(depositorName)) {
            changes.put(PaymentDetailConstants.depositorNameParamName, depositorName);
        }
        if (StringUtils.isNotBlank(depositorNationality)) {
            changes.put(PaymentDetailConstants.depositorNationalityParamName, depositorNationality);
        }
        if (StringUtils.isNotBlank(depositorIdType)) {
            changes.put(PaymentDetailConstants.depositorIdTypeParamName, depositorIdType);
        }
        if (StringUtils.isNotBlank(depositorIdNumber)) {
            changes.put(PaymentDetailConstants.depositorIdNumberParamName, depositorIdNumber);
        }
        if (StringUtils.isNotBlank(depositorIdIssuedAt)) {
            changes.put(PaymentDetailConstants.depositorIdIssuedAtParamName, depositorIdIssuedAt);
        }
        if (StringUtils.isNotBlank(depositorPurpose)) {
            changes.put(PaymentDetailConstants.depositorPurposeParamName, depositorPurpose);
        }
        changes.put("paymentTypeId", paymentType.getId());
        return new PaymentDetail(paymentType, accountNumber, checkNumber, routingCode, receiptNumber, bankNumber,
                depositorName, depositorNationality, depositorIdType, depositorIdNumber, depositorIdIssuedAt, depositorPurpose);
    }

    public static PaymentDetail instance(final PaymentType paymentType, final String accountNumber, final String checkNumber,
            final String routingCode, final String receiptNumber, final String bankNumber) {
        return new PaymentDetail(paymentType, accountNumber, checkNumber, routingCode, receiptNumber, bankNumber,
                null, null, null, null, null, null);
    }

    private PaymentDetail(final PaymentType paymentType, final String accountNumber, final String checkNumber, final String routingCode,
            final String receiptNumber, final String bankNumber, final String depositorName, final String depositorNationality,
            final String depositorIdType, final String depositorIdNumber, final String depositorIdIssuedAt, final String depositorPurpose) {
        this.paymentType = paymentType;
        this.accountNumber = accountNumber;
        this.checkNumber = checkNumber;
        this.routingCode = routingCode;
        this.receiptNumber = receiptNumber;
        this.bankNumber = bankNumber;
        this.depositorName = depositorName;
        this.depositorNationality = depositorNationality;
        this.depositorIdType = depositorIdType;
        this.depositorIdNumber = depositorIdNumber;
        this.depositorIdIssuedAt = depositorIdIssuedAt;
        this.depositorPurpose = depositorPurpose;
    }

    public PaymentDetailData toData() {
        final PaymentTypeData paymentTypeData = null; // this.paymentType.toData();
        return new PaymentDetailData(getId(), paymentTypeData, this.accountNumber, this.checkNumber, this.routingCode, this.receiptNumber,
                this.bankNumber, this.depositorName, this.depositorNationality, this.depositorIdType, this.depositorIdNumber,
                this.depositorIdIssuedAt, this.depositorPurpose);
    }
}
