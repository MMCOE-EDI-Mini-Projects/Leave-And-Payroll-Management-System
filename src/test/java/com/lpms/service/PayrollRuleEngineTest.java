package com.lpms.service;

import com.lpms.model.EmployeeSalaryDetail;
import com.lpms.model.SalaryComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayrollRuleEngineTest {

    private PayrollRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PayrollRuleEngine();
    }

    @Test
    void testCalculateFlatComponent() {
        SalaryComponent component = new SalaryComponent();
        component.setCalculationMethod("FLAT");
        
        EmployeeSalaryDetail detail = new EmployeeSalaryDetail();
        detail.setComponentValue(new BigDecimal("2000.00")); // e.g., Flat 2000 allowance

        BigDecimal result = engine.calculateComponentAmount(component, detail, new BigDecimal("50000.00"));
        assertEquals(new BigDecimal("2000.00"), result);
    }

    @Test
    void testCalculatePercentageComponent() {
        SalaryComponent component = new SalaryComponent();
        component.setCalculationMethod("PERCENTAGE");
        
        EmployeeSalaryDetail detail = new EmployeeSalaryDetail();
        detail.setComponentValue(new BigDecimal("12.00")); // e.g., 12% PF

        // 12% of 50000 = 6000
        BigDecimal result = engine.calculateComponentAmount(component, detail, new BigDecimal("50000.00"));
        assertEquals(new BigDecimal("6000.00"), result);
    }

    @Test
    void testCalculateLopDeduction() {
        // Gross: 60000, Working Days: 30, LOP: 2 days. 
        // per day = 2000. 2 days = 4000
        BigDecimal gross = new BigDecimal("60000.00");
        BigDecimal workingDays = new BigDecimal("30");
        BigDecimal lopDays = new BigDecimal("2");

        BigDecimal result = engine.calculateLopDeduction(gross, workingDays, lopDays);
        assertEquals(new BigDecimal("4000.00"), result);
    }
}
