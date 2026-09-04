/*
#Full code: demo.motor.util.drivingLicenceYears

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "org.smartparam.engine.core.context.ParamContext"
*/


// driver's current age
def driverAge = ctx.getInteger('quote.driver.age')

// driver's age when obtaining licence
def obtainLicenceAge = ctx.getInteger('quote.driver.licenceObtainedAtAge')

// diff
return driverAge - obtainLicenceAge
