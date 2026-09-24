package com.lpms.service;

import com.lpms.model.EmployeeSalary;
import com.lpms.model.EmployeeSalaryDetail;
import com.lpms.model.SalaryComponent;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PayrollRuleEngine applies business rules to calculate salary components.
 * It uses a simplified strategy pattern based on the component's calculation method.
 */
public class PayrollRuleEngine {

    /**
     * Calculates the amount for a specific salary component.
     * 
     * @param component The SalaryComponent configuration
     * @param detail The Employee's specific detail containing the configuration value
     * @param monthlyGross The monthly gross salary of the employee
     * @return The calculated amount
     */
    public BigDecimal calculateComponentAmount(SalaryComponent component, EmployeeSalaryDetail detail, BigDecimal monthlyGross) {
        if (component == null || detail == null) {
            return BigDecimal.ZERO;
        }

        switch (component.getCalculationMethod()) {
            case "FLAT":
                // componentValue is the exact flat amount
                return detail.getComponentValue();

            case "PERCENTAGE":
                // componentValue represents a percentage (e.g., 12.00 for 12%)
                // Formula: (monthlyGross * percentage) / 100
                BigDecimal percentage = detail.getComponentValue();
                return monthlyGross.multiply(percentage)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            case "FORMULA":
                // Simplified formula implementation (Extensible Strategy)
                // In a full implementation, this might parse an AST or expression string
                // For this project scope, we assume it's custom logic implemented here.
                return evaluateCustomFormula(component, detail, monthlyGross);

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal evaluateCustomFormula(SalaryComponent component, EmployeeSalaryDetail detail, BigDecimal monthlyGross) {
        // Mock custom formula for academic scope
        // e.g., If component is "BONUS", give 5% of gross up to a max cap
        if ("BONUS".equals(component.getComponentCode())) {
            BigDecimal bonus = monthlyGross.multiply(new BigDecimal("0.05"));
            BigDecimal cap = new BigDecimal("5000.00");
            return bonus.compareTo(cap) > 0 ? cap : bonus;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculates Loss of Pay deduction.
     * 
     * @param monthlyGross Total monthly gross salary
     * @param workingDays Total working days in the month
     * @param lopDays Loss of Pay days
     * @return The LOP deduction amount
     */
    public BigDecimal calculateLopDeduction(BigDecimal monthlyGross, BigDecimal workingDays, BigDecimal lopDays) {
        if (workingDays.compareTo(BigDecimal.ZERO) == 0 || lopDays.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // perDaySalary = monthlyGross / workingDays
        // LOP = perDaySalary * lopDays
        BigDecimal perDaySalary = monthlyGross.divide(workingDays, 2, RoundingMode.HALF_UP);
        return perDaySalary.multiply(lopDays).setScale(2, RoundingMode.HALF_UP);
    }
}
