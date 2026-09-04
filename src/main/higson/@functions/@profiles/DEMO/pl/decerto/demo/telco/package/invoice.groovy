/*
#Full code: pl.decerto.demo.telco.package.invoice

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

def invoiceValues = type.paramValue().withTypes("number", "number", "date")
  .withCodes("OverallPrice", "OverallPriceAfterDiscounts", "PaymentDate")
  
def paymentDate = date.currentDate()

def coll = []
for(int i = 1; i < 13; i++) {
    log.info("calculating entry:{}", i)
    def invoiceCtx = higson.createContext(ctx, 'invoice.entryNumber', i)
    def pv = higson.getParamValue("pl.decerto.demo.telco.package.invoice.prices", invoiceCtx)
    
        invoiceValues = invoiceValues.addValues(pv.getBigDecimal("OverallPrice"), 
             pv.getBigDecimal("OverallPriceAfterDiscounts"),
             date.getLastDayOfMonth(paymentDate))
        
        paymentDate = date.addMonth(paymentDate, 1)
}

return invoiceValues.build()
