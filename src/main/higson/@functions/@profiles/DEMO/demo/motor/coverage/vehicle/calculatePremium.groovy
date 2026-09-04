/*
#Full code: demo.motor.coverage.vehicle.calculatePremium

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "org.smartparam.engine.core.context.ParamContext"

[[arguments]]
name = "factor"
type = "OBJECT"
*/

// utitily function
// calculated premium depends on
//  - vehicle's engine capacity (in cm3)
//  - vehicle's engine power (in hp)
//  - provided factor


// get vehicle model specification from parameter
def model = higson.getValue('demo.motor.dict.vehicle.availableModels', ctx)

// read capacity OUT column from parameter's result [cm3]
def capacity = model.getNumber('capacity')

// read power OUT column from parameter's result [horse power]
def power = model.getNumber('power')

// use formula
def premium = new BigDecimal(factor) * (capacity / 14 + power / 3)

// return as BigDecimal, 2-digit rounding
return math.round(premium, 2)
