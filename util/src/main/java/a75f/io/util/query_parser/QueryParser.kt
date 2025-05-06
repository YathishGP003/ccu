package a75f.io.util.query_parser

import CcuQueryLexer
import CcuQueryParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.TokenStreamRewriter

fun modifyKVPairFromFilter(filter: String): String {

    val queryFilter = removeFirstAndLastParentheses(filter)
    val lexer = CcuQueryLexer(CharStreams.fromString(queryFilter))
    val token = CommonTokenStream(lexer)
    val parser = CcuQueryParser(token)
    val tree = parser.filter()
    val tokenRewriter = TokenStreamRewriter(token)
    val visitor = CcuQueryRewriter(tokenRewriter)
    visitor.visit(tree)
    return tokenRewriter.text
}

private fun removeFirstAndLastParentheses(inputQuery: String): String {
    var input = inputQuery
    input = input.replace("@@".toRegex(), "@")
    return if (input.startsWith("(") && input.endsWith(")")) {
        input.substring(1, input.length - 1)
    } else {
        input
    }
}