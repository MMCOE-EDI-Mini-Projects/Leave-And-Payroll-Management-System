package com.lpms.model;

import java.math.BigDecimal;

public class SalaryComponent {
    private Long componentId;
    private String componentCode;
    private String componentName;
    private String componentType; // EARNING, DEDUCTION, EMPLOYER_CONTRIBUTION
    private String calculationMethod; // FLAT, PERCENTAGE, FORMULA
    private boolean isTaxable;
    private boolean isPfApplicable;
    private boolean isEsiApplicable;
    private boolean isActive;

    public SalaryComponent() {}

    public Long getComponentId() { return componentId; }
    public void setComponentId(Long componentId) { this.componentId = componentId; }

    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }

    public String getCalculationMethod() { return calculationMethod; }
    public void setCalculationMethod(String calculationMethod) { this.calculationMethod = calculationMethod; }

    public boolean isTaxable() { return isTaxable; }
    public void setTaxable(boolean taxable) { isTaxable = taxable; }

    public boolean isPfApplicable() { return isPfApplicable; }
    public void setPfApplicable(boolean pfApplicable) { isPfApplicable = pfApplicable; }

    public boolean isEsiApplicable() { return isEsiApplicable; }
    public void setEsiApplicable(boolean esiApplicable) { isEsiApplicable = esiApplicable; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
