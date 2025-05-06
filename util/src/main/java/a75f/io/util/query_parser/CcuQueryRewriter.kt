package a75f.io.util.query_parser

import CcuQueryBaseVisitor
import org.antlr.v4.runtime.TokenStreamRewriter

class CcuQueryRewriter(private val rewriter: TokenStreamRewriter) : CcuQueryBaseVisitor<Unit>() {

    /**
         This method is called when the parser visits a comparison term
         If the value contains "@SYSTEM", this is mostly relevant for roomRef and floorRef
         Since in CCU this SYSTEM could be prefixed with @ after being replaced, we need to run both cases
         as part of the query string
         If value is other than @SYSTEM, we need to format the value only.
     */
    override fun visitComparison_term(ctx: CcuQueryParser.Comparison_termContext?) {
        val key = ctx?.IDENTIFIER()?.text ?: return
        val scalarValue = ctx.scalar()
        scalarValue?.let { scalarContext ->
             if(scalarContext.text == "@SYSTEM") {
                 val operator = ctx.comparison_operator()?.text ?: return
                 val newQuery = " (" + key + " " + operator + " " + convertToStrScalar(scalarContext.text) +
                        " or " +
                        key + " " + operator + " " + convertToStrScalar(scalarContext.text.removePrefix("@")) + ") "
                rewriter.replace(ctx.start, ctx.stop, newQuery)
            } else {
                formatScalarValue(scalarContext, key)?.let { value ->
                    rewriter.replace(scalarValue.start, scalarValue.stop, value)
                }
            }
        }
        super.visitComparison_term(ctx)
    }

    /**
        * This method is called when the parser visits a scalar value
        *
        * If the value is a reference scalar, we need to format it to a string scalar
        * If the value is a quoted string, we do not need to change it
        * If the key ends with "Ref" or "Refs", we do not need to change it
        * If the key is "id", we do not need to change it
        * In other scenario where string value is expected but @ is prefixed, the @ is removed
        * and the value is converted to a string scalar
        * @param scalarValue The scalar value context from the parser
        * @param key The key associated with the scalar value
        * @return The formatted scalar value or null if no formatting is needed
     */
    private fun formatScalarValue(scalarValue: CcuQueryParser.ScalarContext, key: String): String? {
        var value = scalarValue.text
        val isQuoted = scalarValue.str_scalar() != null
        if(isQuoted || key == "id") {
            return null
        }
        val isRefScalar = scalarValue.ref_scalar() != null
        if(isRefScalar) {
            if(!key.endsWith("Ref") && !key.endsWith("Refs")) {
                value = scalarValue.text.removePrefix("@")
            }
            return convertToStrScalar(value)
        }
        return null
    }

    private fun convertToStrScalar(valueString: String): String {
        return "\"$valueString\""
    }
}