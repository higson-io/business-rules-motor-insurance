/*
#Full code: pl.decerto.demo.telco.extra.channel.price.calculator

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
name = "packageCode"
type = "STRING"

[[arguments]]
name = "duration"
type = "INTEGER"
*/

def packageRoot = domain.get("TELCO").get("/Packages[" + packageCode + "]")
BigDecimal channelsPrice = BigDecimal.ZERO

for (def extraChannel : ctx.get("package.extraChannels")) {
    if (extraChannel.getBoolean("selected")) {
        String code = extraChannel.get("code")
        def channelContext = higson.createContext("extraChannel.code", code,
        "package.code", packageCode,
        "package.duration", duration)
        
        BigDecimal price = packageRoot.get("EXTRA_CHANNELS", code).getAttrDecimal("Price", channelContext)
        log.info("adding selected channel:{} with price:{}", code, price)
        
        channelsPrice = channelsPrice.add(price)
    }
}
log.info("calculated extra channels price:{}", channelsPrice)

return channelsPrice