/*
#Full code: pl.decerto.demo.telco.service.price.discount.calculator

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

def discountPercentValue = 0.0
def isDiscountAvailable = higson.call('pl.decerto.demo.telco.isExtraChannelSelected', ctx) //zwraca true lub false zależnie od dostępności zniżki za polączone pakiety

if(isDiscountAvailable){
     discountPercentValue = higson.getNumber('pl.decerto.demo.telco.serviceAndChannel.discount', ctx)
}

log.info('discount percent value:{}', discountPercentValue)

def monthlyPriceWithoutDiscounts = higson.getNumber('pl.decerto.demo.telco.service.monthly.price', ctx)
log.info('phone service price before discount:{}', monthlyPriceWithoutDiscounts)

def monthlyPriceAfterDiscount = monthlyPriceWithoutDiscounts * (1.0-discountPercentValue/100)

monthlyPriceAfterDiscount = math.round(monthlyPriceAfterDiscount,2)

log.info('calculated discount price for service:{}', monthlyPriceAfterDiscount)

return monthlyPriceAfterDiscount