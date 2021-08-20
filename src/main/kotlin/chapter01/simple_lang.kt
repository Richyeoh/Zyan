package chapter01

enum class TokenType {
    IDENTIFIER,
    KEYWORD,
    SEPARATOR,
    STRING,
    EOF
}

class Token(var type: TokenType, var value: String)

/*
    fun foo(){}
    fun sayHello(){
        foo()
        println("hello, world!")
    }
    sayHello()
    println("first","second","three")
 */
val tokenList = arrayListOf(
    Token(TokenType.KEYWORD, "fun"),
    Token(TokenType.IDENTIFIER, "foo"),
    Token(TokenType.SEPARATOR, "("),
    Token(TokenType.SEPARATOR, ")"),
    Token(TokenType.SEPARATOR, "{"),
    Token(TokenType.SEPARATOR, "}"),
    Token(TokenType.KEYWORD, "fun"),
    Token(TokenType.IDENTIFIER, "sayHello"),
    Token(TokenType.SEPARATOR, "("),
    Token(TokenType.SEPARATOR, ")"),
    Token(TokenType.SEPARATOR, "{"),
    Token(TokenType.IDENTIFIER, "foo"),
    Token(TokenType.SEPARATOR, "("),
    Token(TokenType.SEPARATOR, ")"),
    Token(TokenType.IDENTIFIER, "println"),
    Token(TokenType.SEPARATOR, "("),
    Token(TokenType.STRING, "hello, world!"),
    Token(TokenType.SEPARATOR, ")"),
    Token(TokenType.SEPARATOR, "}"),
    Token(TokenType.IDENTIFIER, "sayHello"),
    Token(TokenType.SEPARATOR, "("),
    Token(TokenType.SEPARATOR, ")"),
    Token(TokenType.IDENTIFIER, "println"),
    Token(TokenType.SEPARATOR, "("),
    Token(TokenType.STRING, "first"),
    Token(TokenType.SEPARATOR, ","),
    Token(TokenType.STRING, "second"),
    Token(TokenType.SEPARATOR, ","),
    Token(TokenType.STRING, "three"),
    Token(TokenType.SEPARATOR, ")"),
    Token(TokenType.EOF, "")
)

class Tokenizer(private val tokenList: List<Token>) {
    private var position: Int = 0

    fun next(): Token {
        return if (position <= tokenList.size) {
            tokenList[position++]
        } else {
            tokenList[position]
        }
    }

    fun peek(): Token {
        return tokenList[position]
    }

    fun position(): Int {
        return position
    }

    fun trackBack(newPosition: Int) {
        position = newPosition
    }
}

abstract class AstNode {
    abstract fun dump()
}

abstract class Statement : AstNode()

class Program(val statementList: List<Statement>) : AstNode() {
    override fun dump() {
        println("program: ")
        statementList.forEach { statement ->
            statement.dump()
        }
    }
}

class FunctionDeclare(
    val name: String,
    val body: FunctionBody
) : Statement() {
    override fun dump() {
        println("\t" + "function declare: " + name)
        body.dump()
    }
}

class FunctionBody(val calls: List<FunctionCall>) : Statement() {
    override fun dump() {
        calls.forEach { functionCall ->
            functionCall.dump()
        }
    }
}

class FunctionCall(
    val name: String,
    val parameterList: List<String>,
    var definition: FunctionDeclare? = null
) : Statement() {
    override fun dump() {
        println("\t" + "function call: " + name + ", parameters: " + parameterList.joinToString(" "))
    }
}

/*
    FunctionDeclare: "function" Identifier ( Parameter* ) FunctionBody
    FunctionBody: "{" FunctionCall* "}"
    FunctionCall: Identifier "(" ParameterList? ")"
 */
class Parser(private val tokenizer: Tokenizer) {
    fun parseProgram(): Program {
        val statementList = arrayListOf<Statement>()

        var statement: Statement? = null

        while (true) {
            statement = parseFunctionDeclare()
            if (statement != null) {
                statementList.add(statement)
                continue
            }

            statement = parseFunctionCall()
            if (statement != null) {
                statementList.add(statement)
                continue
            }

            if (statement == null) {
                break
            }
        }

        if (statementList.isEmpty()) {
            throw IllegalArgumentException()
        }

        return Program(statementList)
    }

    fun parseFunctionDeclare(): FunctionDeclare? {
        println("start parseFunctionDeclare")

        val position = tokenizer.position()

        val token0 = tokenizer.next()
        if (token0.type == TokenType.KEYWORD && token0.value == "fun") {
            val token1 = tokenizer.next()
            if (token1.type == TokenType.IDENTIFIER) {
                val token2 = tokenizer.next()
                if (token2.value == "(") {
                    val token3 = tokenizer.next()
                    if (token3.value == ")") {
                        val functionBody = parseFunctionBody()
                        if (functionBody != null) {
                            return FunctionDeclare(token1.value, functionBody)
                        }
                    }
                }
            }
        }

        tokenizer.trackBack(position)
        return null
    }

    fun parseFunctionBody(): FunctionBody? {
        println("start parseFunctionBody")

        val position = tokenizer.position()

        val statementList = arrayListOf<FunctionCall>()
        val token0 = tokenizer.next()
        if (token0.value == "{") {
            var functionCall = parseFunctionCall()
            while (functionCall != null) {
                statementList.add(functionCall)
                functionCall = parseFunctionCall()
            }
            val token1 = tokenizer.next()
            if (token1.value == "}") {
                return FunctionBody(statementList)
            }
        }

        tokenizer.trackBack(position)
        return null
    }

    fun parseFunctionCall(): FunctionCall? {
        println("start parseFunctionCall")

        val position = tokenizer.position()

        val parameterList = arrayListOf<String>()
        val token0 = tokenizer.next()
        if (token0.type == TokenType.IDENTIFIER) {
            val token1 = tokenizer.next()
            if (token1.value == "(") {
                var token2 = tokenizer.next()
                while (token2.value != ")") {
                    if (token2.type == TokenType.STRING) {
                        parameterList.add(token2.value)
                    }
                    token2 = tokenizer.next()
                    if (token2.value != ")") {
                        if (token2.value == ",") {
                            token2 = tokenizer.next()
                        }
                    }
                }
                return FunctionCall(token0.value, parameterList)
            }
        }

        tokenizer.trackBack(position)
        return null
    }
}

class RefResolver(private val program: Program) {
    fun visitProgram(): Program {
        val statementList = program.statementList
        statementList.forEach { statement ->
            when (statement) {
                is FunctionDeclare -> {
                    visitFunctionDeclare(statement)
                }

                is FunctionCall -> {
                    resolveFunctionCall(statement)
                }
            }
        }
        return program
    }

    fun resolveFunctionCall(call: FunctionCall) {
        if (call.name == "println") {
            println("${call.name} is resolved")
            return
        }

        val declare = findFunctionCallDefinition(call.name)
        if (declare != null) {
            call.definition = declare
            println("${call.name} is resolved")
        }
    }

    fun findFunctionCallDefinition(functionName: String): FunctionDeclare? {
        val statementList = program.statementList
        for (statement in statementList) {
            if (statement is FunctionDeclare && statement.name == functionName) {
                return statement
            }
        }
        return null
    }

    fun visitFunctionDeclare(declare: FunctionDeclare) {
        visitFunctionBody(declare.body)
    }

    fun visitFunctionBody(body: FunctionBody) {
        val calls = body.calls
        calls.forEach { call ->
            visitFunctionCall(call)
            resolveFunctionCall(call)
        }
    }

    fun visitFunctionCall(call: FunctionCall) {
        println("${call.name} is visited")
    }
}

class Interpreter(private val program: Program) {
    fun run() {
        val statementList = program.statementList
        statementList.forEach { statement ->
            val functionCall = statement as? FunctionCall
            if (functionCall != null) {
                println("${functionCall.name} invoked")
                runFunction(functionCall)
            }
        }
    }

    fun runFunction(functionCall: FunctionCall) {
        if (functionCall.name == "println") {
            if (functionCall.parameterList.isNotEmpty()) {
                println(functionCall.parameterList.joinToString(" "))
            } else {
                println()
            }
            return
        }

        val functionDefinition = functionCall.definition
        if (functionDefinition != null) {
            val body = functionDefinition.body
            body.calls.forEach { call ->
                runFunction(call)
            }
        } else {
            println("function " + functionCall.name + " not defined")
        }
    }
}

fun main() {
    val tokenizer = Tokenizer(tokenList)
    val parser = Parser(tokenizer)
    val program = parser.parseProgram()
    program.dump()
    val resolvedProgram = RefResolver(program).visitProgram()
    Interpreter(resolvedProgram).run()
}
