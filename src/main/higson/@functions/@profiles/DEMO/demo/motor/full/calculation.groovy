/*
#Full code: demo.motor.full.calculation

# DO NOT REMOVE, MOVE OR MODIFY THIS COMMENT
# unless you know what you're doing. Incorrect content will cause error during import.
# IMPORTANT: these escape sequences MUST be used inside this comment: '/' -> '\/', '\' -> '\\'

categories = []
logLevel = "INFO"
description = ""

[[arguments]]
name = "ctx"
type = "SPECIFIED_CLASS"
className = "io.higson.runtime.engine.core.context.ParamContext"
*/

def plans = domain.get("DEMO", "/PLANS[FULL]")

def options = plans.getAll("OPTIONS")
def allCoverages = plans.getAll("COVERAGES") 
def allDiscounts = plans.getAll("DISCOUNTS")

def result = []
for (def option : options) {
    def coverages = createCoverages(option,allCoverages)
    def discounts = createDiscounts(option,allDiscounts)
    def sumOfPremiums = coverages.sum(0) { it["premium"] ?: 0 }
    def sumOfDiscounts = discounts.sum(0) { it["value"] ?: 0 }
    result.add([
            "option": [
                    "code"                  : option.code,
                    "coverages"             : coverages,
                    "discounts"             : discounts,
                    "order"                 : option.getAttrInteger("ORDER", ctx),
                    "premiumBeforeDiscounts": sumOfPremiums,
                    "premium"               : sumOfPremiums - sumOfDiscounts,
            ]
    ])
}
result

def createCoverages(option, allCoverages) {
    def coveragesResult = []
    
    for (def coverage : allCoverages) {
        def adhocCtx = higson.createContext(ctx, "coverage.code", coverage.code, "option.code", option.code)
        
        if (!coverage.getAttrBoolean("IS_AVAILABLE", adhocCtx)) {
            continue;
        }
        
        def limit1 = coverage.getAttrInteger("LIMIT_1", adhocCtx)
        
        adhocCtx = higson.createContext(ctx, "coverage.code", coverage.code, "option.code", option.code, "coverage.limit1", limit1)
        coveragesResult.add([
                "code"              : coverage.code,
                "name"              : coverage.name,
                "description"       : coverage.getAttrString("DESCRIPTION", adhocCtx),
                "position"          : coverage.getAttrInteger("POSITION", adhocCtx),
                "limitLeft"         : limit1,
                "limitRight"        : coverage.getAttrInteger("LIMIT_2", adhocCtx),
                "premium"           : coverage.getAttrDecimal("PREMIUM", adhocCtx),
                "riskAssessment"    : coverage.getAttrDecimal("RISK_ASSESSMENT", ctx)?.with { math.round(it, 3)}
        ])
    }
    coveragesResult
}

def createDiscounts(option, allDiscounts) {
    def discountsResult = []
    def adhocCtx = higson.createContext(ctx, "option.code", option.code)
    
    for (def discount : allDiscounts) {
        if (!discount.getAttrBoolean("available", adhocCtx)) {
            continue;
        }
        discountsResult.add([
                "code"    : discount.code,
                "name"    : discount.name,
                "position": discount.getAttrInteger("position", adhocCtx),
                "value"   : discount.getAttrDecimal("value", adhocCtx),
        ])
    }
    discountsResult
}