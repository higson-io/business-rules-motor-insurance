/*
#Full code: pl.decerto.demo.telco.isExtraChannelSelected

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


def packageCode = ctx.getString('package.code')
def extraChannelsForDiscount = higson.getString('pl.decerto.demo.telco.serviceAndChannel.discount.channelsForDiscount', ctx)
def selectedExtraChannels = ctx.get('package.extraChannels')

def isDiscountAvaliable = selectedExtraChannels.any{extraChannel ->
    extraChannelsForDiscount.contains(extraChannel.code) && extraChannel.selected}

return isDiscountAvaliable

